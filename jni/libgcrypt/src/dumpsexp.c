/* dumpsexp.c - Dump S-expressions.
 * Copyright (C) 2007 Free Software Foundation, Inc.
 *
 * Getrandom is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * Getrandom is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

#include <config.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include <stdarg.h>
#include <errno.h>

#define PGM "dumpsexp"
#define MYVERSION_LINE PGM " (Libgcrypt) " VERSION
#define BUGREPORT_LINE "\nReport bugs to <bug-libgcrypt@gnupg.org>.\n"


static int verbose;  /* Verbose mode.  */
static int decimal;  /* Print addresses in decimal.  */
static int assume_hex;  /* Assume input is hexencoded.  */

static void
print_version (int with_help)
{
  fputs (MYVERSION_LINE "\n"
         "Copyright (C) 2007 Free Software Foundation, Inc.\n"
         "License GPLv2+: GNU GPL version 2 or later "
         "<http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>\n"
         "This is free software: you are free to change and redistribute it.\n"
         "There is NO WARRANTY, to the extent permitted by law.\n",
         stdout);
        
  if (with_help)
    fputs ("\n"
           "Usage: " PGM " [OPTIONS] [file]\n"
           "Debug tool for S-expressions\n"
           "\n"
           "  --decimal     Print offsets using decimal notation\n"
           "  --assume-hex  Assume input is a hex dump\n"
           "  --verbose     Show what we are doing\n"
           "  --version     Print version of the program and exit\n"
           "  --help        Display this help and exit\n"
           BUGREPORT_LINE, stdout );
  
  exit (0);
}

static int
print_usage (void)
{
  fputs ("usage: " PGM " [OPTIONS] NBYTES\n", stderr);
  fputs ("       (use --help to display options)\n", stderr);
  exit (1);
}


#define space_p(a)    ((a)==' ' || (a)=='\n' || (a)=='\r' || (a)=='\t')
#define digit_p(a)    ((a) >= '0' && (a) <= '9')
#define octdigit_p(a) ((a) >= '0' && (a) <= '7')
#define alpha_p(a)    (   ((a) >= 'A' && (a) <= 'Z')  \
                       || ((a) >= 'a' && (a) <= 'z'))
#define hexdigit_p(a) (digit_p (a)                     \
                       || ((a) >= 'A' && (a) <= 'F')  \
                       || ((a) >= 'a' && (a) <= 'f'))
#define xtoi_1(a)     ((a) <= '9'? ((a)- '0'): \
                       (a) <= 'F'? ((a)-'A'+10):((a)-'a'+10))


/* Return true if P points to a byte containing a whitespace according
   to the S-expressions definition. */
static inline int
whitespace_p (int c)
{ 
  switch (c)
    {
    case ' ': case '\t': case '\v': case '\f': case '\r': case '\n': return 1;
    default: return 0;
    }
}

static void
logit (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format) ;
  fputs (PGM ": ", stderr);
  vfprintf (stderr, format, arg_ptr);
  putc ('\n', stderr);
  va_end (arg_ptr);
}

/* The raw data buffer and its current length */
static unsigned char databuffer[16];
static int databufferlen;
/* The number of bytes in databuffer which should be skipped at a flush.  */
static int skipdatabufferlen;
/* The number of raw bytes printed on the last line.  */
static int nbytesprinted;
/* The file offset of the current data buffer .  */
static unsigned long databufferoffset;



static int
my_getc (FILE *fp)
{
  int c1, c2;

  if (!assume_hex)
    return getc (fp);

  while ( (c1=getc (fp)) != EOF && space_p (c1) )
    ;
  if (c1 == EOF)
    return EOF;

  if (!hexdigit_p (c1))
    {
      logit ("non hex-digit encountered\n");
      return EOF;
    }

  while ( (c2=getc (fp)) != EOF && space_p (c2) )
    ;
  if (c2 == EOF)
    {
      logit ("error reading second hex nibble\n");
      return EOF;
    }
  if (!hexdigit_p (c2))
    {
      logit ("second hex nibble is not a hex-digit\n");
      return EOF;
    }
  return xtoi_1 (c1) * 16 + xtoi_1 (c2);
}





/* Flush the raw data buffer.  */
static void
flushdatabuffer (void)
{
  int i;

  if (!databufferlen)
    return;
  nbytesprinted = 0;
  if (decimal)
    printf ("%08lu ", databufferoffset);
  else
    printf ("%08lx ", databufferoffset);
  for (i=0; i < databufferlen; i++)
    {
      if (i == 8)
        putchar (' ');
      if (i < skipdatabufferlen)
        fputs ("   ", stdout);
      else
        {
          printf (" %02x", databuffer[i]);
          databufferoffset++;
        }
      nbytesprinted++;
    }
  for (; i < sizeof (databuffer); i++)
    {
      if (i == 8)
        putchar (' ');
      fputs ("   ", stdout);
    }
  fputs ("  |", stdout);
  for (i=0; i < databufferlen; i++)
    {
      if (i < skipdatabufferlen)
        putchar (' ');
      else if (databuffer[i] >= ' ' && databuffer[i] <= '~'
               && databuffer[i] != '|')
        putchar (databuffer[i]);
      else
        putchar ('.');
    }
  putchar ('|');
  putchar ('\n');
  databufferlen = 0;
  skipdatabufferlen = 0;
}


/* Add C to the raw data buffer and flush as needed.  */
static void
addrawdata (int c)
{
  if ( databufferlen >= sizeof databuffer )
    flushdatabuffer ();
  databuffer[databufferlen++] = c;
}


static void 
printcursor (int both)
{
  int i;
 
  flushdatabuffer ();
  printf ("%8s ", "");
  for (i=0; i < sizeof (databuffer); i++)
    {
      if (i == 8)
        putchar (' ');
      if (i+1 == nbytesprinted)
        {
          fputs (" ^ ", stdout);
          if (!both)
            break;
        }
      else
        fputs ("   ", stdout);
    }
  if (both)
    {
      fputs ("   ", stdout);
      for (i=0; i < nbytesprinted-1; i++)
        putchar (' ');
      putchar ('^');
    }
  databufferlen = skipdatabufferlen = nbytesprinted;
}

static void 
printerr (const char *text)
{
  printcursor (1);
  printf ("\n          Error: %s\n", text);
}

static void 
printctl (const char *text)
{
  if (verbose)
    {
      printcursor (0);
      printf ("%s\n", text);
    }
}

static void 
printchr (int c)
{
  (void)c;
}

static void
printhex (int c)
{
  (void)c;
}






static int
parse_and_print (FILE *fp)
{
  static const char tokenchars[] =
    "abcdefghijklmnopqrstuvwxyz"
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "0123456789-./_:*+=";
  int c;
  int level = 0;
  int tokenc = 0;
  int hexcount = 0;
  int disphint = 0;
  unsigned long datalen = 0;
  char quote_buf[10];
  int quote_idx = 0;
  enum 
    {
      INIT_STATE = 0, IN_NUMBER, PRE_DATA, IN_DATA, IN_STRING,
      IN_ESCAPE, IN_OCT_ESC, IN_HEX_ESC,
      CR_ESC, LF_ESC, IN_HEXFMT, IN_BASE64
    }
  state = INIT_STATE;
  

  while ((c = my_getc (fp)) != EOF )
    {
      addrawdata (c);
      switch (state)
        {
        case INIT_STATE:
          if (tokenc)
            {
              if (strchr (tokenchars, c))
                {
                  printchr (c);
                  continue;
                }
              tokenc = 0;
            }
        parse_init_state:
          if (c == '(')
            {
              if (disphint)
                {
                  printerr ("unmatched display hint");
                  disphint = 0;
                }
              printctl ("open");
              level++;
            }
          else if (c == ')')
            {
              if (disphint)
                {
                  printerr ("unmatched display hint");
                  disphint = 0;
                }
              printctl ("close");
              level--;
            }
          else if (c == '\"')
            {
              state = IN_STRING;
              printctl ("beginstring");
            }
          else if (c == '#')
            {
              state = IN_HEXFMT;
              hexcount = 0;
              printctl ("beginhex");
            }
          else if (c == '|')
            {
              state = IN_BASE64;
              printctl ("beginbase64");
            }
          else if (c == '[')
            {
              if (disphint)
                printerr ("nested display hint");
              disphint = c;
            }
          else if (c == ']')
            {
              if (!disphint)
                printerr ("no open display hint");
              disphint = 0;
            }
          else if (c >= '0' && c <= '9')
            {
              if (c == '0')
                printerr ("zero prefixed length");
              state = IN_NUMBER;
              datalen = (c - '0');
            }
          else if (strchr (tokenchars, c))
            {
              printchr (c);
              tokenc = c;
            }
          else if (whitespace_p (c))
            ;
          else if (c == '{')
            {
              printerr ("rescanning is not supported");
            }
          else if (c == '&' || c == '\\')
            {
              printerr ("reserved punctuation detected");
            }
          else
            {
              printerr ("bad character detected");
            }
          break;

        case IN_NUMBER:
          if (digit_p (c))
            {
              unsigned long tmp = datalen * 10 + (c - '0');
              if (tmp < datalen)
                {
                  printerr ("overflow in data length");
                  state = INIT_STATE;
                  datalen = 0;
                }
              else
                datalen = tmp;
            }
          else if (c == ':')
            {
              if (!datalen)
                {
                  printerr ("no data length");
                  state = INIT_STATE;
                }
              else
                state = PRE_DATA;
            }
          else if (c == '\"' || c == '#' || c == '|' )
            {
              /* We ignore the optional length and divert to the init
                 state parser code. */
              goto parse_init_state;
            }
          else
            printerr ("invalid length specification");
          break;

        case PRE_DATA:
          state = IN_DATA;
          printctl ("begindata");
        case IN_DATA:
          if (datalen)
            {
              printhex (c);
              datalen--;
            }
          if (!datalen)
            {
              state = INIT_STATE;
              printctl ("enddata");
            }
          break;

        case IN_STRING:
          if (c == '\"')
            {
              printctl ("endstring");
              state = INIT_STATE;
            } 
          else if (c == '\\')
            state = IN_ESCAPE;
          else
            printchr (c);
          break;

        case IN_ESCAPE:
          switch (c)
            {
            case 'b': case 't': case 'v': case 'n': case 'f':
            case 'r': case '"': case '\'': case '\\':
              printhex (c);
              state = IN_STRING;
              break;
              
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7':
              state = IN_OCT_ESC;
              quote_idx = 0;
              quote_buf[quote_idx++] = c; 
              break;
              
            case 'x':
              state = IN_HEX_ESC;
              quote_idx = 0;
              break;
              
            case '\r':
              state = CR_ESC;
              break;
              
            case '\n':
              state = LF_ESC;
              break;

            default:
              printerr ("invalid escape sequence");
              state = IN_STRING;
              break;
            }
          
        case IN_OCT_ESC: 
          state = IN_STRING;
          break;
        case IN_HEX_ESC: 
          state = IN_STRING;
          break;
        case CR_ESC:
          state = IN_STRING;
          break;
        case LF_ESC:
          state = IN_STRING;
          break;

        case IN_HEXFMT:
          if (hexdigit_p (c))
            {
              printchr (c);
              hexcount++;
            }
          else if (c == '#')
            {
              if ((hexcount & 1))
                printerr ("odd number of hex digits");
              printctl ("endhex");
              state = INIT_STATE;
            }
          else if (!whitespace_p (c))
            printerr ("bad hex character");
          break;

        case IN_BASE64:
          if (c == '|')
            {
              printctl ("endbase64");
              state = INIT_STATE;
            }
          else
            printchr (c);
          break;

        default:
          logit ("invalid state %d detected", state);
          exit (1);
        }
    }
  flushdatabuffer ();
  if (ferror (fp))
    {
      logit ("error reading input: %s\n", strerror (errno));
      return -1;
    }
  return 0;
}



int 
main (int argc, char **argv)
{
  int rc;

  if (argc)
    {
      argc--; argv++;
    }
  while (argc && **argv == '-' && (*argv)[1] == '-')
    {
      if (!(*argv)[2])
        {
          argc--; argv++;
          break;
        }
      else if (!strcmp (*argv, "--version"))
        print_version (0);
      else if (!strcmp (*argv, "--help"))
        print_version (1);
      else if (!strcmp (*argv, "--verbose"))
        {
          argc--; argv++;
          verbose = 1;
        }
      else if (!strcmp (*argv, "--decimal"))
        {
          argc--; argv++;
          decimal = 1;
        }
      else if (!strcmp (*argv, "--assume-hex"))
        {
          argc--; argv++;
          assume_hex = 1;
        }
      else
        print_usage ();
    }          

  if (!argc)
    {
      rc = parse_and_print (stdin);
    }
  else
    {
      for (; argc; argc--)
        {
          FILE *fp = fopen (*argv, "rb");
          if (!fp)
            {
              logit ("can't open `%s': %s\n", *argv, strerror (errno));
              rc = 1;
            }
          else 
            {
              if ( parse_and_print (fp) )
                rc = 1;
              fclose (fp);
            }
        }
    }
  

  return !rc;
}

