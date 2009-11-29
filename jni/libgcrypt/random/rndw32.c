/* rndw32.c  -  W32 entropy gatherer
 * Copyright (C) 1999, 2000, 2002, 2003, 2007 Free Software Foundation, Inc.
 * Copyright Peter Gutmann, Matt Thomlinson and Blake Coverett 1996-2006
 *
 * This file is part of Libgcrypt.
 *
 *************************************************************************
 * The code here is based on code from Cryptlib 3.0 beta by Peter Gutmann.
 * Source file misc/rndwin32.c "Win32 Randomness-Gathering Code" with this
 * copyright notice:
 *
 * This module is part of the cryptlib continuously seeded pseudorandom
 * number generator.  For usage conditions, see lib_rand.c
 *
 * [Here is the notice from lib_rand.c, which is now called dev_sys.c]
 *
 * This module and the misc/rnd*.c modules represent the cryptlib
 * continuously seeded pseudorandom number generator (CSPRNG) as described in
 * my 1998 Usenix Security Symposium paper "The generation of random numbers
 * for cryptographic purposes".
 *
 * The CSPRNG code is copyright Peter Gutmann (and various others) 1996,
 * 1997, 1998, 1999, all rights reserved.  Redistribution of the CSPRNG
 * modules and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice
 *    and this permission notice in its entirety.
 *
 * 2. Redistributions in binary form must reproduce the copyright notice in
 *    the documentation and/or other materials provided with the distribution.
 *
 * 3. A copy of any bugfixes or enhancements made must be provided to the
 *    author, <pgut001@cs.auckland.ac.nz> to allow them to be added to the
 *    baseline version of the code.
 *
 * ALTERNATIVELY, the code may be distributed under the terms of the
 * GNU Lesser General Public License, version 2.1 or any later version
 * published by the Free Software Foundation, in which case the
 * provisions of the GNU LGPL are required INSTEAD OF the above
 * restrictions.
 *
 * Although not required under the terms of the LGPL, it would still
 * be nice if you could make any changes available to the author to
 * allow a consistent code base to be maintained.
 *************************************************************************
 * The above alternative was changed from GPL to LGPL on 2007-08-22 with
 * permission from Peter Gutmann:
 *==========
 From: pgut001 <pgut001@cs.auckland.ac.nz>
 Subject: Re: LGPL for the windows entropy gatherer
 To: wk@gnupg.org
 Date: Wed, 22 Aug 2007 03:05:42 +1200

 Hi,

 >As of now libgcrypt is GPL under Windows due to that module and some people
 >would really like to see it under LGPL too.  Can you do such a license change
 >to LGPL version 2?  Note that LGPL give the user the option to relicense it
 >under GPL, so the change would be pretty easy and backwar compatible.

 Sure.  I assumed that since GPG was GPLd, you'd prefer the GPL for the entropy
 code as well, but Ian asked for LGPL as an option so as of the next release
 I'll have LGPL in there.  You can consider it to be retroactive, so your
 current version will be LGPLd as well.

 Peter.
 *==========
 */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#ifdef __GNUC__  
#include <stdint.h>
#endif

#include <windows.h>


#include "types.h"
#include "g10lib.h"
#include "rand-internal.h"


/* Definitions which are missing from the current GNU Windows32Api.  */
#ifndef IOCTL_DISK_PERFORMANCE
#define IOCTL_DISK_PERFORMANCE  0x00070020
#endif

/* This used to be (6*8+5*4+8*2), but Peter Gutmann figured a larger
   value in a newer release. So we use a far larger value. */
#define SIZEOF_DISK_PERFORMANCE_STRUCT 256

/* We don't include wincrypt.h so define it here.  */
#define HCRYPTPROV  HANDLE


/* When we query the performance counters, we allocate an initial buffer and
 * then reallocate it as required until RegQueryValueEx() stops returning
 * ERROR_MORE_DATA.  The following values define the initial buffer size and
 * step size by which the buffer is increased
 */
#define PERFORMANCE_BUFFER_SIZE         65536   /* Start at 64K */
#define PERFORMANCE_BUFFER_STEP         16384   /* Step by 16K */


/* The number of bytes to read from the system RNG on each slow poll.  */
#define SYSTEMRNG_BYTES 64

/* Intel Chipset CSP type and name */
#define PROV_INTEL_SEC  22
#define INTEL_DEF_PROV  "Intel Hardware Cryptographic Service Provider"




/* Type definitions for function pointers to call NetAPI32 functions.  */
typedef DWORD (WINAPI *NETSTATISTICSGET)(LPWSTR szServer, LPWSTR szService,
                                         DWORD dwLevel, DWORD dwOptions,
                                         LPBYTE *lpBuffer);
typedef DWORD (WINAPI *NETAPIBUFFERSIZE)(LPVOID lpBuffer, LPDWORD cbBuffer);
typedef DWORD (WINAPI *NETAPIBUFFERFREE)(LPVOID lpBuffer);

/* Type definitions for function pointers to call native NT functions.  */
typedef DWORD (WINAPI *NTQUERYSYSTEMINFORMATION)(DWORD systemInformationClass,
                                                 PVOID systemInformation,
                                                 ULONG systemInformationLength,
                                                 PULONG returnLength);
typedef DWORD (WINAPI *NTQUERYINFORMATIONPROCESS)
     (HANDLE processHandle, DWORD processInformationClass,
      PVOID processInformation, ULONG processInformationLength,
      PULONG returnLength);
typedef DWORD (WINAPI *NTPOWERINFORMATION)
     (DWORD powerInformationClass, PVOID inputBuffer,
      ULONG inputBufferLength, PVOID outputBuffer, ULONG outputBufferLength );

/* Type definitions for function pointers to call CryptoAPI functions. */
typedef BOOL (WINAPI *CRYPTACQUIRECONTEXT)(HCRYPTPROV *phProv,
                                           LPCTSTR pszContainer,
                                           LPCTSTR pszProvider, 
                                           DWORD dwProvType,
                                           DWORD dwFlags);
typedef BOOL (WINAPI *CRYPTGENRANDOM)(HCRYPTPROV hProv, DWORD dwLen,
                                      BYTE *pbBuffer);
typedef BOOL (WINAPI *CRYPTRELEASECONTEXT)(HCRYPTPROV hProv, DWORD dwFlags);

/* Somewhat alternative functionality available as a direct call, for 
   Windows XP and newer.  This is the CryptoAPI RNG, which isn't anywhere
   near as good as the HW RNG, but we use it if it's present on the basis
   that at least it can't make things any worse.  This direct access version 
   is only available under Windows XP, we don't go out of our way to access
   the more general CryptoAPI one since the main purpose of using it is to 
   take advantage of any possible future hardware RNGs that may be added, 
   for example via TCPA devices.  */
typedef BOOL (WINAPI *RTLGENRANDOM)(PVOID RandomBuffer, 
                                    ULONG RandomBufferLength);



/* MBM data structures, originally by Alexander van Kaam, converted to C by
   Anders@Majland.org, finally updated by Chris Zahrt <techn0@iastate.edu> */
#define BusType         char
#define SMBType         char
#define SensorType      char

typedef struct 
{
  SensorType iType;               /* Type of sensor.  */
  int Count;                      /* Number of sensor for that type.  */
} SharedIndex;

typedef struct 
{
  SensorType ssType;              /* Type of sensor */
  unsigned char ssName[12];       /* Name of sensor */
  char sspadding1[3];             /* Padding of 3 bytes */
  double ssCurrent;               /* Current value */
  double ssLow;                   /* Lowest readout */
  double ssHigh;                  /* Highest readout */
  long ssCount;                   /* Total number of readout */
  char sspadding2[4];             /* Padding of 4 bytes */
  long double ssTotal;            /* Total amout of all readouts */
  char sspadding3[6];             /* Padding of 6 bytes */
  double ssAlarm1;                /* Temp & fan: high alarm; voltage: % off */
  double ssAlarm2;                /* Temp: low alarm */
} SharedSensor;

typedef struct
{
  short siSMB_Base;               /* SMBus base address */
  BusType siSMB_Type;             /* SMBus/Isa bus used to access chip */
  SMBType siSMB_Code;             /* SMBus sub type, Intel, AMD or ALi */
  char siSMB_Addr;                /* Address of sensor chip on SMBus */
  unsigned char siSMB_Name[41];   /* Nice name for SMBus */
  short siISA_Base;               /* ISA base address of sensor chip on ISA */
  int siChipType;                 /* Chip nr, connects with Chipinfo.ini */
  char siVoltageSubType;          /* Subvoltage option selected */
} SharedInfo;

typedef struct
{
  double sdVersion;               /* Version number (example: 51090) */
  SharedIndex sdIndex[10];        /* Sensor index */
  SharedSensor sdSensor[100];     /* Sensor info */
  SharedInfo sdInfo;              /* Misc.info */
  unsigned char sdStart[41];      /* Start time */

  /* We don't use the next two fields both because they're not random
     and because it provides a nice safety margin in case of data size
     mis- estimates (we always under-estimate the buffer size).  */
#if 0
  unsigned char sdCurrent[41];    /* Current time */
  unsigned char sdPath[256];      /* MBM path */
#endif /*0*/
} SharedData;



/* One time intialized handles and function pointers.  We use dynamic
   loading of the DLLs to do without them in case libgcrypt does not
   need any random.  */
static HANDLE hNetAPI32;
static NETSTATISTICSGET pNetStatisticsGet;
static NETAPIBUFFERSIZE pNetApiBufferSize;
static NETAPIBUFFERFREE pNetApiBufferFree;

static HANDLE hNTAPI;
static NTQUERYSYSTEMINFORMATION  pNtQuerySystemInformation;
static NTQUERYINFORMATIONPROCESS pNtQueryInformationProcess;
static NTPOWERINFORMATION        pNtPowerInformation;

static HANDLE hAdvAPI32;
static CRYPTACQUIRECONTEXT pCryptAcquireContext;
static CRYPTGENRANDOM      pCryptGenRandom;
static CRYPTRELEASECONTEXT pCryptReleaseContext;
static RTLGENRANDOM        pRtlGenRandom;


/* Other module global variables.  */
static int system_rng_available; /* Whether a system RNG is available.  */
static HCRYPTPROV hRNGProv;      /* Handle to Intel RNG CSP. */

static int debug_me;  /* Debug flag.  */




/* Try and connect to the system RNG if there's one present. */
static void 
init_system_rng (void)
{
  system_rng_available = 0;
  hRNGProv = NULL;

  hAdvAPI32 = GetModuleHandle ("AdvAPI32.dll");
  if (!hAdvAPI32)
    return;

  pCryptAcquireContext = (CRYPTACQUIRECONTEXT)
    GetProcAddress (hAdvAPI32, "CryptAcquireContextA");
  pCryptGenRandom = (CRYPTGENRANDOM)
    GetProcAddress (hAdvAPI32, "CryptGenRandom");
  pCryptReleaseContext = (CRYPTRELEASECONTEXT)
    GetProcAddress (hAdvAPI32, "CryptReleaseContext");
  
  /* Get a pointer to the native randomness function if it's available.  
     This isn't exported by name, so we have to get it by ordinal.  */
  pRtlGenRandom = (RTLGENRANDOM)
    GetProcAddress (hAdvAPI32, "SystemFunction036");

  /* Try and connect to the PIII RNG CSP.  The AMD 768 southbridge (from 
     the 760 MP chipset) also has a hardware RNG, but there doesn't appear 
     to be any driver support for this as there is for the Intel RNG so we 
     can't do much with it.  OTOH the Intel RNG is also effectively dead 
     as well, mostly due to virtually nonexistant support/marketing by 
     Intel, it's included here mostly for form's sake.  */
  if ( (!pCryptAcquireContext || !pCryptGenRandom || !pCryptReleaseContext
        || !pCryptAcquireContext (&hRNGProv, NULL, INTEL_DEF_PROV,
                                  PROV_INTEL_SEC, 0) )
       && !pRtlGenRandom)
    {
      hAdvAPI32 = NULL;
    }
  else
    system_rng_available = 1;
}


/* Read data from the system RNG if availavle.  */
static void 
read_system_rng (void (*add)(const void*, size_t, enum random_origins),
                 enum random_origins requester)
{
  BYTE buffer[ SYSTEMRNG_BYTES + 8 ];
  int quality = 0;

  if (!system_rng_available)
    return;

  /* Read SYSTEMRNG_BYTES bytes from the system RNG.  We don't rely on
     this for all our randomness requirements (particularly the
     software RNG) in case it's broken in some way.  */
  if (hRNGProv)
    {
      if (pCryptGenRandom (hRNGProv, SYSTEMRNG_BYTES, buffer))
        quality = 80;
    }
  else if (pRtlGenRandom)
    {
      if ( pRtlGenRandom (buffer, SYSTEMRNG_BYTES))
        quality = 50;
    }
  if (quality > 0)
    {
      if (debug_me)
        log_debug ("rndw32#read_system_rng: got %d bytes of quality %d\n",
                   SYSTEMRNG_BYTES, quality);
      (*add) (buffer, SYSTEMRNG_BYTES, requester);
      wipememory (buffer, SYSTEMRNG_BYTES);
    }
}


/* Read data from MBM.  This communicates via shared memory, so all we
   need to do is map a file and read the data out.  */
static void
read_mbm_data (void (*add)(const void*, size_t, enum random_origins), 
               enum random_origins requester)
{
  HANDLE hMBMData;
  SharedData *mbmDataPtr;

  hMBMData = OpenFileMapping (FILE_MAP_READ, FALSE, "$M$B$M$5$S$D$" );
  if (hMBMData)
    {
      mbmDataPtr = (SharedData*)MapViewOfFile (hMBMData, FILE_MAP_READ,0,0,0);
      if (mbmDataPtr)
        {
          if (debug_me)
            log_debug ("rndw32#read_mbm_data: got %d bytes\n",
                       (int)sizeof (SharedData));
          (*add) (mbmDataPtr, sizeof (SharedData), requester);
          UnmapViewOfFile (mbmDataPtr);
        }
      CloseHandle (hMBMData);
    }
}


/* Fallback method using the registry to poll the statistics.  */
static void
registry_poll (void (*add)(const void*, size_t, enum random_origins), 
               enum random_origins requester)
{
  static int cbPerfData = PERFORMANCE_BUFFER_SIZE;
  int iterations;
  DWORD dwSize, status;
  PERF_DATA_BLOCK *pPerfData;

  /* Get information from the system performance counters.  This can take a
     few seconds to do.  In some environments the call to RegQueryValueEx()
     can produce an access violation at some random time in the future, in
     some cases adding a short delay after the following code block makes
     the problem go away.  This problem is extremely difficult to
     reproduce, I haven't been able to get it to occur despite running it
     on a number of machines.  MS knowledge base article Q178887 covers
     this type of problem, it's typically caused by an external driver or
     other program that adds its own values under the
     HKEY_PERFORMANCE_DATA key.  The NT kernel, via Advapi32.dll, calls the
     required external module to map in the data inside an SEH try/except
     block, so problems in the module's collect function don't pop up until
     after it has finished, so the fault appears to occur in Advapi32.dll.
     There may be problems in the NT kernel as well though, a low-level
     memory checker indicated that ExpandEnvironmentStrings() in
     Kernel32.dll, called an interminable number of calls down inside
     RegQueryValueEx(), was overwriting memory (it wrote twice the
     allocated size of a buffer to a buffer allocated by the NT kernel).
     OTOH this could be coming from the external module calling back into
     the kernel, which eventually causes the problem described above.

     Possibly as an extension of the problem that the krnlWaitSemaphore()
     call above works around, running two instances of cryptlib (e.g. two
     applications that use it) under NT4 can result in one of them hanging
     in the RegQueryValueEx() call.  This happens only under NT4 and is
     hard to reproduce in any consistent manner.

     One workaround that helps a bit is to read the registry as a remote
     (rather than local) registry, it's possible that the use of a network
     RPC call isolates the calling app from the problem in that whatever
     service handles the RPC is taking the hit and not affecting the
     calling app.  Since this would require another round of extensive
     testing to verify and the NT native API call is working fine, we'll
     stick with the native API call for now.

     Some versions of NT4 had a problem where the amount of data returned
     was mis-reported and would never settle down, because of this the code
     below includes a safety-catch that bails out after 10 attempts have
     been made, this results in no data being returned but at does ensure
     that the thread will terminate.

     In addition to these problems the code in RegQueryValueEx() that
     estimates the amount of memory required to return the performance
     counter information isn't very accurate (it's much worse than the
     "slightly-inaccurate" level that the MS docs warn about, it's usually
     wildly off) since it always returns a worst-case estimate which is
     usually nowhere near the actual amount required.  For example it may
     report that 128K of memory is required, but only return 64K of data.

     Even worse than the registry-based performance counters is the
     performance data helper (PDH) shim that tries to make the counters
     look like the old Win16 API (which is also used by Win95).  Under NT
     this can consume tens of MB of memory and huge amounts of CPU time
     while it gathers its data, and even running once can still consume
     about 1/2MB of memory */
  pPerfData = gcry_xmalloc (cbPerfData);
  for (iterations=0; iterations < 10; iterations++)
    {
      dwSize = cbPerfData;
      if ( debug_me )
        log_debug ("rndw32#slow_gatherer_nt: get perf data\n" );

      status = RegQueryValueEx (HKEY_PERFORMANCE_DATA, "Global", NULL,
                                NULL, (LPBYTE) pPerfData, &dwSize);
      if (status == ERROR_SUCCESS)
        {
          if (!memcmp (pPerfData->Signature, L"PERF", 8))
            (*add) ( pPerfData, dwSize, requester );
          else
            log_debug ("rndw32: no PERF signature\n");
          break;
        }
      else if (status == ERROR_MORE_DATA)
        {
          cbPerfData += PERFORMANCE_BUFFER_STEP;
          pPerfData = gcry_xrealloc (pPerfData, cbPerfData);
        }
      else
        {
          static int been_here;

          /* Silence the error message.  In particular under Wine (as
             of 2008) we would get swamped with such diagnotiscs.  One
             such diagnotiscs should be enough.  */
          if (been_here != status)
            {
              been_here = status;
              log_debug ("rndw32: get performance data problem: ec=%ld\n",
                         status);
            }
          break;
        }
    }
  gcry_free (pPerfData);

  /* Although this isn't documented in the Win32 API docs, it's necessary
     to explicitly close the HKEY_PERFORMANCE_DATA key after use (it's
     implicitly opened on the first call to RegQueryValueEx()).  If this
     isn't done then any system components which provide performance data
     can't be removed or changed while the handle remains active.  */
  RegCloseKey (HKEY_PERFORMANCE_DATA);
}


static void
slow_gatherer ( void (*add)(const void*, size_t, enum random_origins), 
                enum random_origins requester )
{
  static int is_initialized = 0;
  static int is_workstation = 1;
  HANDLE hDevice;
  DWORD dwType, dwSize, dwResult;
  ULONG ulSize;
  int drive_no, status;
  int no_results = 0;
  void *buffer;

  if ( !is_initialized )
    {
      HKEY hKey;

      if ( debug_me )
        log_debug ("rndw32#slow_gatherer: init toolkit\n" );
      /* Find out whether this is an NT server or workstation if necessary */
      if (RegOpenKeyEx (HKEY_LOCAL_MACHINE,
                        "SYSTEM\\CurrentControlSet\\Control\\ProductOptions",
                        0, KEY_READ, &hKey) == ERROR_SUCCESS)
        {
          BYTE szValue[32 + 8];
          dwSize = 32;

          if ( debug_me )
            log_debug ("rndw32#slow_gatherer: check product options\n" );

          status = RegQueryValueEx (hKey, "ProductType", 0, NULL,
                                    szValue, &dwSize);
          if (status == ERROR_SUCCESS && stricmp (szValue, "WinNT"))
            {
              /* Note: There are (at least) three cases for ProductType:
                 WinNT = NT Workstation, ServerNT = NT Server, LanmanNT =
                 NT Server acting as a Domain Controller.  */
              is_workstation = 0;
              if ( debug_me )
                log_debug ("rndw32: this is a NT server\n");
            }
          RegCloseKey (hKey);
        }

      /* The following are fixed for the lifetime of the process so we
         only add them once */
      /* readPnPData ();  - we have not implemented that.  */

      /* Initialize the NetAPI32 function pointers if necessary */
      hNetAPI32 = LoadLibrary ("NETAPI32.DLL");
      if (hNetAPI32)
        {
          if (debug_me)
            log_debug ("rndw32#slow_gatherer: netapi32 loaded\n" );
          pNetStatisticsGet = (NETSTATISTICSGET)
            GetProcAddress (hNetAPI32, "NetStatisticsGet");
          pNetApiBufferSize = (NETAPIBUFFERSIZE)
            GetProcAddress (hNetAPI32, "NetApiBufferSize");
          pNetApiBufferFree = (NETAPIBUFFERFREE)
            GetProcAddress (hNetAPI32, "NetApiBufferFree");

          if (!pNetStatisticsGet || !pNetApiBufferSize || !pNetApiBufferFree)
            {
              FreeLibrary (hNetAPI32);
              hNetAPI32 = NULL;
              log_debug ("rndw32: No NETAPI found\n" );
            }
        }

      /* Initialize the NT kernel native API function pointers if necessary */
      hNTAPI = GetModuleHandle ("NTDll.dll");
      if (hNTAPI)
        {
          /* Get a pointer to the NT native information query functions */
          pNtQuerySystemInformation = (NTQUERYSYSTEMINFORMATION)
            GetProcAddress (hNTAPI, "NtQuerySystemInformation");
          pNtQueryInformationProcess = (NTQUERYINFORMATIONPROCESS)
            GetProcAddress (hNTAPI, "NtQueryInformationProcess");
          pNtPowerInformation = (NTPOWERINFORMATION)
            GetProcAddress(hNTAPI, "NtPowerInformation");

          if (!pNtQuerySystemInformation || !pNtQueryInformationProcess)
            hNTAPI = NULL;
        }


      is_initialized = 1;
    }
  
  read_system_rng ( add, requester );
  read_mbm_data ( add, requester );
  
  /* Get network statistics.    Note: Both NT Workstation and NT Server by
     default will be running both the workstation and server services.  The
     heuristic below is probably useful though on the assumption that the
     majority of the network traffic will be via the appropriate service.
     In any case the network statistics return almost no randomness.  */
  {
    LPBYTE lpBuffer;
    
    if (hNetAPI32
        && !pNetStatisticsGet (NULL,
                               is_workstation ? L"LanmanWorkstation" :
                               L"LanmanServer", 0, 0, &lpBuffer))
      {
        if ( debug_me )
          log_debug ("rndw32#slow_gatherer: get netstats\n" );
        pNetApiBufferSize (lpBuffer, &dwSize);
        (*add) ( lpBuffer, dwSize, requester );
        pNetApiBufferFree (lpBuffer);
      }
  }

  /* Get disk I/O statistics for all the hard drives.  100 is an
     arbitrary failsafe limit.  */
  for (drive_no = 0; drive_no < 100 ; drive_no++)
    {
      char diskPerformance[SIZEOF_DISK_PERFORMANCE_STRUCT + 8];
      char szDevice[50];
      
      /* Check whether we can access this device.  */
      snprintf (szDevice, sizeof szDevice, "\\\\.\\PhysicalDrive%d",
                drive_no);
      hDevice = CreateFile (szDevice, 0, FILE_SHARE_READ | FILE_SHARE_WRITE,
                            NULL, OPEN_EXISTING, 0, NULL);
      if (hDevice == INVALID_HANDLE_VALUE)
        break; /* No more drives.  */
        
      /* Note: This only works if you have turned on the disk performance
         counters with 'diskperf -y'.  These counters are off by default. */
      dwSize = sizeof diskPerformance;
      if (DeviceIoControl (hDevice, IOCTL_DISK_PERFORMANCE, NULL, 0,
                           diskPerformance, SIZEOF_DISK_PERFORMANCE_STRUCT,
                           &dwSize, NULL))
        {
          if ( debug_me )
            log_debug ("rndw32#slow_gatherer: iostat drive %d\n",
                       drive_no);
          (*add) (diskPerformance, dwSize, requester);
        }
      else
        {
          log_info ("NOTE: you should run 'diskperf -y' "
                    "to enable the disk statistics\n");
        }
      CloseHandle (hDevice);
    }

  /* In theory we should be using the Win32 performance query API to obtain
     unpredictable data from the system, however this is so unreliable (see
     the multiple sets of comments in registryPoll()) that it's too risky
     to rely on it except as a fallback in emergencies.  Instead, we rely
     mostly on the NT native API function NtQuerySystemInformation(), which
     has the dual advantages that it doesn't have as many (known) problems
     as the Win32 equivalent and that it doesn't access the data indirectly
     via pseudo-registry keys, which means that it's much faster.  Note
     that the Win32 equivalent actually works almost all of the time, the
     problem is that on one or two systems it can fail in strange ways that
     are never the same and can't be reproduced on any other system, which
     is why we use the native API here.  Microsoft officially documented
     this function in early 2003, so it'll be fairly safe to use.  */
  if ( !hNTAPI )
    {
      registry_poll (add, requester);
      return;
    }


  /* Scan the first 64 possible information types (we don't bother with
     increasing the buffer size as we do with the Win32 version of the
     performance data read, we may miss a few classes but it's no big deal).
     This scan typically yields around 20 pieces of data, there's nothing
     in the range 65...128 so chances are there won't be anything above
     there either.  */
  buffer = gcry_xmalloc (PERFORMANCE_BUFFER_SIZE);
  for (dwType = 0; dwType < 64; dwType++)
    {
      switch (dwType)
        {
          /* Some information types are write-only (the IDs are shared with
             a set-information call), we skip these.  */
        case 26: case 27: case 38: case 46: case 47: case 48: case 52:
          continue;

          /* ID 53 = SystemSessionProcessInformation reads input from the
             output buffer, which has to contain a session ID and pointer
             to the actual buffer in which to store the session information.
             Because this isn't a standard query, we skip this.  */
        case  53:
          continue;
        }

      /* Query the info for this ID.  Some results (for example for
         ID = 6, SystemCallCounts) are only available in checked builds
         of the kernel.  A smaller subcless of results require that
         certain system config flags be set, for example
         SystemObjectInformation requires that the
         FLG_MAINTAIN_OBJECT_TYPELIST be set in NtGlobalFlags.  To avoid
         having to special-case all of these, we try reading each one and
         only use those for which we get a success status.  */
      dwResult = pNtQuerySystemInformation (dwType, buffer,
                                            PERFORMANCE_BUFFER_SIZE - 2048,
                                            &ulSize);
      if (dwResult != ERROR_SUCCESS)
        continue;

      /* Some calls (e.g. ID = 23, SystemProcessorStatistics, and ID = 24,
         SystemDpcInformation) incorrectly return a length of zero, so we
         manually adjust the length to the correct value.  */
      if ( !ulSize )
        {
          if (dwType == 23)
            ulSize = 6 * sizeof (ULONG);
          else if (dwType == 24)
            ulSize = 5 * sizeof (ULONG);
        }

      /* If we got some data back, add it to the entropy pool.  */
      if (ulSize > 0 && ulSize <= PERFORMANCE_BUFFER_SIZE - 2048)
        {
          if (debug_me)
            log_debug ("rndw32#slow_gatherer: %lu bytes from sysinfo %ld\n",
                       ulSize, dwType);
          (*add) (buffer, ulSize, requester);
          no_results++;
        }
    }

  /* Now we would do the same for the process information.  This
     call would rather ugly in that it requires an exact length
     match for the data returned, failing with a
     STATUS_INFO_LENGTH_MISMATCH error code (0xC0000004) if the
     length isn't an exact match.  It requires a compiler to handle
     complex nested structs, alignment issues, and so on, and
     without the headers in which the entries are declared it's
     almost impossible to do.  Thus we don't.  */


  /* Finally, do the same for the system power status information.  There
     are only a limited number of useful information types available so we
     restrict ourselves to the useful types.  In addition since this
     function doesn't return length information, we have to hardcode in
     length data.  */
  if (pNtPowerInformation)
    {
      static const struct { int type; int size; } powerInfo[] = {
        { 0, 128 },     /* SystemPowerPolicyAc */
        { 1, 128 },     /* SystemPowerPolicyDc */
        { 4, 64 },      /* SystemPowerCapabilities */
        { 5, 48 },      /* SystemBatteryState */
        { 11, 48 },     /* ProcessorInformation */
        { 12, 24 },     /* SystemPowerInformation */
        { -1, -1 }
      };
      int i;

      /* The 100 is a failsafe limit.  */
      for (i = 0; powerInfo[i].type != -1 && i < 100; i++ )
        {
          /* Query the info for this ID */
          dwResult = pNtPowerInformation (powerInfo[i].type, NULL, 0, buffer,
                                          PERFORMANCE_BUFFER_SIZE - 2048);
          if (dwResult != ERROR_SUCCESS)
            continue;
          if (debug_me)
            log_debug ("rndw32#slow_gatherer: %u bytes from powerinfo %d\n",
                       powerInfo[i].size, i);
          (*add) (buffer, powerInfo[i].size, requester);
          no_results++;
        }
      gcry_assert (i < 100);
    }
  gcry_free (buffer);

  /* We couldn't get enough results from the kernel, fall back to the
     somewhat troublesome registry poll.  */
  if (no_results < 15)
    registry_poll (add, requester);
}


int
_gcry_rndw32_gather_random (void (*add)(const void*, size_t,
                                        enum random_origins),
                            enum random_origins origin,
                            size_t length, int level )
{
  static int is_initialized;

  if (!level)
    return 0;

  /* We don't differentiate between level 1 and 2 here because there
     is no internal entropy pool as a scary resource.  It may all work
     slower, but because our entropy source will never block but
     deliver some not easy to measure entropy, we assume level 2.  */

  if (!is_initialized)
    {
      OSVERSIONINFO osvi = { sizeof( osvi ) };

      GetVersionEx( &osvi );
      if ( osvi.dwPlatformId != VER_PLATFORM_WIN32_NT)
        log_fatal ("can only run on a Windows NT platform\n" );
      init_system_rng ();
      is_initialized = 1;
    }

  if (debug_me)
    log_debug ("rndw32#gather_random: ori=%d len=%u lvl=%d\n",
               origin, (unsigned int)length, level );

  slow_gatherer (add, origin);

  return 0;
}



void
_gcry_rndw32_gather_random_fast (void (*add)(const void*, size_t,
                                             enum random_origins),
                                 enum random_origins origin)
{
  static int addedFixedItems = 0;

  if ( debug_me )
    log_debug ("rndw32#gather_random_fast: ori=%d\n", origin );

  /* Get various basic pieces of system information: Handle of active
     window, handle of window with mouse capture, handle of clipboard
     owner handle of start of clpboard viewer list, pseudohandle of
     current process, current process ID, pseudohandle of current
     thread, current thread ID, handle of desktop window, handle of
     window with keyboard focus, whether system queue has any events,
     cursor position for last message, 1 ms time for last message,
     handle of window with clipboard open, handle of process heap,
     handle of procs window station, types of events in input queue,
     and milliseconds since Windows was started.  */

  {
    byte buffer[20*sizeof(ulong)], *bufptr;

    bufptr = buffer;
#define ADD(f)  do { ulong along = (ulong)(f);                  \
                     memcpy (bufptr, &along, sizeof (along) );  \
                     bufptr += sizeof (along);                  \
                   } while (0)

    ADD ( GetActiveWindow ());
    ADD ( GetCapture ());
    ADD ( GetClipboardOwner ());
    ADD ( GetClipboardViewer ());
    ADD ( GetCurrentProcess ());
    ADD ( GetCurrentProcessId ());
    ADD ( GetCurrentThread ());
    ADD ( GetCurrentThreadId ());
    ADD ( GetDesktopWindow ());
    ADD ( GetFocus ());
    ADD ( GetInputState ());
    ADD ( GetMessagePos ());
    ADD ( GetMessageTime ());
    ADD ( GetOpenClipboardWindow ());
    ADD ( GetProcessHeap ());
    ADD ( GetProcessWindowStation ());
    ADD ( GetQueueStatus (QS_ALLEVENTS));
    ADD ( GetTickCount ());

    gcry_assert ( bufptr-buffer < sizeof (buffer) );
    (*add) ( buffer, bufptr-buffer, origin );
#undef ADD
  }

  /* Get multiword system information: Current caret position, current
     mouse cursor position.  */
  {
    POINT point;

    GetCaretPos (&point);
    (*add) ( &point, sizeof (point), origin );
    GetCursorPos (&point);
    (*add) ( &point, sizeof (point), origin );
  }

  /* Get percent of memory in use, bytes of physical memory, bytes of
     free physical memory, bytes in paging file, free bytes in paging
     file, user bytes of address space, and free user bytes.  */
  {
    MEMORYSTATUS memoryStatus;

    memoryStatus.dwLength = sizeof (MEMORYSTATUS);
    GlobalMemoryStatus (&memoryStatus);
    (*add) ( &memoryStatus, sizeof (memoryStatus), origin );
  }

  /* Get thread and process creation time, exit time, time in kernel
     mode, and time in user mode in 100ns intervals.  */
  {
    HANDLE handle;
    FILETIME creationTime, exitTime, kernelTime, userTime;
    DWORD minimumWorkingSetSize, maximumWorkingSetSize;

    handle = GetCurrentThread ();
    GetThreadTimes (handle, &creationTime, &exitTime,
                    &kernelTime, &userTime);
    (*add) ( &creationTime, sizeof (creationTime), origin );
    (*add) ( &exitTime, sizeof (exitTime), origin );
    (*add) ( &kernelTime, sizeof (kernelTime), origin );
    (*add) ( &userTime, sizeof (userTime), origin );

    handle = GetCurrentProcess ();
    GetProcessTimes (handle, &creationTime, &exitTime,
                     &kernelTime, &userTime);
    (*add) ( &creationTime, sizeof (creationTime), origin );
    (*add) ( &exitTime, sizeof (exitTime), origin );
    (*add) ( &kernelTime, sizeof (kernelTime), origin );
    (*add) ( &userTime, sizeof (userTime), origin );

    /* Get the minimum and maximum working set size for the current
       process.  */
    GetProcessWorkingSetSize (handle, &minimumWorkingSetSize,
                              &maximumWorkingSetSize);
    (*add) ( &minimumWorkingSetSize,
             sizeof (minimumWorkingSetSize), origin );
    (*add) ( &maximumWorkingSetSize,
             sizeof (maximumWorkingSetSize), origin );
  }


  /* The following are fixed for the lifetime of the process so we only
   * add them once */
  if (!addedFixedItems)
    {
      STARTUPINFO startupInfo;

      /* Get name of desktop, console window title, new window
         position and size, window flags, and handles for stdin,
         stdout, and stderr.  */
      startupInfo.cb = sizeof (STARTUPINFO);
      GetStartupInfo (&startupInfo);
      (*add) ( &startupInfo, sizeof (STARTUPINFO), origin );
      addedFixedItems = 1;
    }

  /* The performance of QPC varies depending on the architecture it's
     running on and on the OS, the MS documentation is vague about the
     details because it varies so much.  Under Win9x/ME it reads the
     1.193180 MHz PIC timer.  Under NT/Win2K/XP it may or may not read the
     64-bit TSC depending on the HAL and assorted other circumstances,
     generally on machines with a uniprocessor HAL
     KeQueryPerformanceCounter() uses a 3.579545MHz timer and on machines
     with a multiprocessor or APIC HAL it uses the TSC (the exact time
     source is controlled by the HalpUse8254 flag in the kernel).  That
     choice of time sources is somewhat peculiar because on a
     multiprocessor machine it's theoretically possible to get completely
     different TSC readings depending on which CPU you're currently
     running on, while for uniprocessor machines it's not a problem.
     However, the kernel appears to synchronise the TSCs across CPUs at
     boot time (it resets the TSC as part of its system init), so this
     shouldn't really be a problem.  Under WinCE it's completely platform-
     dependant, if there's no hardware performance counter available, it
     uses the 1ms system timer.
     
     Another feature of the TSC (although it doesn't really affect us here)
     is that mobile CPUs will turn off the TSC when they idle, Pentiums
     will change the rate of the counter when they clock-throttle (to
     match the current CPU speed), and hyperthreading Pentiums will turn
     it off when both threads are idle (this more or less makes sense,
     since the CPU will be in the halted state and not executing any
     instructions to count).
     
     To make things unambiguous, we detect a CPU new enough to call RDTSC
     directly by checking for CPUID capabilities, and fall back to QPC if
     this isn't present.  */
#ifdef __GNUC__  
/*   FIXME: We would need to implement the CPU feature tests first.  */
/*   if (cpu_has_feature_rdtsc) */
/*     { */
/*       uint32_t lo, hi; */
      /* We cannot use "=A", since this would use %rax on x86_64. */
/*       __asm__ __volatile__ ("rdtsc" : "=a" (lo), "=d" (hi)); */
      /* Ignore high 32 bits, hwich are >1s res.  */
/*       (*add) (&lo, 4, origin ); */
/*     } */
/*   else */
#endif /*!__GNUC__*/
    {
      LARGE_INTEGER performanceCount;
      
      if (QueryPerformanceCounter (&performanceCount))
        {
          if ( debug_me )
          log_debug ("rndw32#gather_random_fast: perf data\n");
          (*add) (&performanceCount, sizeof (performanceCount), origin);
        }
      else
        {
          /* Millisecond accuracy at best... */
          DWORD aword = GetTickCount ();
          (*add) (&aword, sizeof (aword), origin );
        }
    }


}
