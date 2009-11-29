/* sexp.c  -  S-Expression handling
 * Copyright (C) 1999, 2000, 2001, 2002, 2003,
 *               2004, 2006, 2007, 2008  Free Software Foundation, Inc.
 *
 * This file is part of Libgcrypt.
 *
 * Libgcrypt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser general Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * Libgcrypt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 */


#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <ctype.h>
#include <errno.h>

#define GCRYPT_NO_MPI_MACROS 1
#include "g10lib.h"
#include "memory.h"

typedef struct gcry_sexp *NODE;
typedef unsigned short DATALEN;

struct gcry_sexp
{
  byte d[1];
};

#define ST_STOP  0
#define ST_DATA  1  /* datalen follows */
#define ST_HINT  2  /* datalen follows */
#define ST_OPEN  3
#define ST_CLOSE 4

/* the atoi macros assume that the buffer has only valid digits */
#define atoi_1(p)   (*(p) - '0' )
#define xtoi_1(p)   (*(p) <= '9'? (*(p)- '0'): \
                     *(p) <= 'F'? (*(p)-'A'+10):(*(p)-'a'+10))
#define xtoi_2(p)   ((xtoi_1(p) * 16) + xtoi_1((p)+1))

#define TOKEN_SPECIALS  "-./_:*+="

static gcry_error_t
sexp_sscan (gcry_sexp_t *retsexp, size_t *erroff,
	    const char *buffer, size_t length, int argflag,
	    va_list arg_ptr, void **arg_list);

/* Return true if P points to a byte containing a whitespace according
   to the S-expressions definition. */
#undef whitespacep
static GPG_ERR_INLINE int
whitespacep (const char *p)
{ 
  switch (*p)
    {
    case ' ': case '\t': case '\v': case '\f': case '\r': case '\n': return 1;
    default: return 0;
    }
}


#if 0
static void
dump_mpi( gcry_mpi_t a )
{
    char buffer[1000];
    size_t n = 1000;

    if( !a )
	fputs("[no MPI]", stderr );
    else if( gcry_mpi_print( GCRYMPI_FMT_HEX, buffer, &n, a ) )
	fputs("[MPI too large to print]", stderr );
    else
	fputs( buffer, stderr );
}
#endif

static void
dump_string (const byte *p, size_t n, int delim )
{
  for (; n; n--, p++ )
    {
      if ((*p & 0x80) || iscntrl( *p ) || *p == delim ) 
        {
          if( *p == '\n' )
            log_printf ("\\n");
          else if( *p == '\r' )
            log_printf ("\\r");
          else if( *p == '\f' )
            log_printf ("\\f");
          else if( *p == '\v' )
            log_printf ("\\v");
	    else if( *p == '\b' )
              log_printf ("\\b");
          else if( !*p )       
            log_printf ("\\0");
          else
            log_printf ("\\x%02x", *p );
	}
      else
        log_printf ("%c", *p);
    }
}


void
gcry_sexp_dump (const gcry_sexp_t a)
{
  const byte *p;
  int indent = 0;
  int type;

  if (!a)
    {
      log_printf ( "[nil]\n");
      return;
    }

  p = a->d;
  while ( (type = *p) != ST_STOP )
    {
      p++;
      switch ( type )
        {
        case ST_OPEN:
          log_printf ("%*s[open]\n", 2*indent, "");
          indent++;
          break;
        case ST_CLOSE:
          if( indent )
            indent--;
          log_printf ("%*s[close]\n", 2*indent, "");
          break;
        case ST_DATA: {
          DATALEN n;
          memcpy ( &n, p, sizeof n );
          p += sizeof n;
          log_printf ("%*s[data=\"", 2*indent, "" );
          dump_string (p, n, '\"' );
          log_printf ("\"]\n");
          p += n;
        }
        break;
        default:
          log_printf ("%*s[unknown tag %d]\n", 2*indent, "", type);
          break;
	}
    }
}

/****************
 * Pass list through except when it is an empty list - in that case
 * return NULL and release the passed list.
 */
static gcry_sexp_t
normalize ( gcry_sexp_t list )
{
  unsigned char *p;

  if ( !list )
    return NULL;
  p = list->d;
  if ( *p == ST_STOP ) 
    {
      /* this is "" */
      gcry_sexp_release ( list );
      return NULL;
    }
  if ( *p == ST_OPEN && p[1] == ST_CLOSE )
    {
      /* this is "()" */
      gcry_sexp_release ( list );
      return NULL;
    }
  
  return list;
}

/* Create a new S-expression object by reading LENGTH bytes from
   BUFFER, assuming it is canonical encoded or autodetected encoding
   when AUTODETECT is set to 1.  With FREEFNC not NULL, ownership of
   the buffer is transferred to the newly created object.  FREEFNC
   should be the freefnc used to release BUFFER; there is no guarantee
   at which point this function is called; most likey you want to use
   free() or gcry_free(). 
 
   Passing LENGTH and AUTODETECT as 0 is allowed to indicate that
   BUFFER points to a valid canonical encoded S-expression.  A LENGTH
   of 0 and AUTODETECT 1 indicates that buffer points to a
   null-terminated string.
  
   This function returns 0 and and the pointer to the new object in
   RETSEXP or an error code in which case RETSEXP is set to NULL.  */
gcry_error_t
gcry_sexp_create (gcry_sexp_t *retsexp, void *buffer, size_t length,
                  int autodetect, void (*freefnc)(void*) )
{
  gcry_error_t errcode;
  gcry_sexp_t se;
  volatile va_list dummy_arg_ptr;

  if (!retsexp)
    return gcry_error (GPG_ERR_INV_ARG);
  *retsexp = NULL;
  if (autodetect < 0 || autodetect > 1 || !buffer)
    return gcry_error (GPG_ERR_INV_ARG);

  if (!length && !autodetect)
    { /* What a brave caller to assume that there is really a canonical
         encoded S-expression in buffer */
      length = gcry_sexp_canon_len (buffer, 0, NULL, &errcode);
      if (!length)
        return errcode;
    }
  else if (!length && autodetect)
    { /* buffer is a string */
      length = strlen ((char *)buffer);
    }

  errcode = sexp_sscan (&se, NULL, buffer, length, 0, dummy_arg_ptr, NULL);
  if (errcode)
    return errcode;

  *retsexp = se;
  if (freefnc)
    {
      /* For now we release the buffer immediately.  As soon as we
         have changed the internal represenation of S-expression to
         the canoncial format - which has the advantage of faster
         parsing - we will use this function as a closure in our
         GCRYSEXP object and use the BUFFER directly.  */
      freefnc (buffer);
    }
  return gcry_error (GPG_ERR_NO_ERROR);
}

/* Same as gcry_sexp_create but don't transfer ownership */
gcry_error_t
gcry_sexp_new (gcry_sexp_t *retsexp, const void *buffer, size_t length,
               int autodetect)
{
  return gcry_sexp_create (retsexp, (void *)buffer, length, autodetect, NULL);
}


/****************
 * Release resource of the given SEXP object.
 */
void
gcry_sexp_release( gcry_sexp_t sexp )
{
  if (sexp)
    {
      if (gcry_is_secure (sexp))
        {
          /* Extra paranoid wiping. */
          const byte *p = sexp->d;
          int type;

          while ( (type = *p) != ST_STOP )
            {
              p++;
              switch ( type )
                {
                case ST_OPEN:
                  break;
                case ST_CLOSE:
                  break;
                case ST_DATA: 
                  {
                    DATALEN n;
                    memcpy ( &n, p, sizeof n );
                    p += sizeof n;
                    p += n;
                  }
                  break;
                default:
                  break;
                }
            }
          wipememory (sexp->d, p - sexp->d);
        }
      gcry_free ( sexp );
    }
}


/****************
 * Make a pair from lists a and b, don't use a or b later on.
 * Special behaviour:  If one is a single element list we put the
 * element straight into the new pair.
 */
gcry_sexp_t
gcry_sexp_cons( const gcry_sexp_t a, const gcry_sexp_t b )
{
  (void)a;
  (void)b;

  /* NYI: Implementation should be quite easy with our new data
     representation */
  BUG ();
  return NULL;
}


/****************
 * Make a list from all items in the array the end of the array is marked
 * with a NULL.
 */
gcry_sexp_t
gcry_sexp_alist( const gcry_sexp_t *array )
{
  (void)array;

  /* NYI: Implementation should be quite easy with our new data
     representation. */
  BUG ();
  return NULL;
}

/****************
 * Make a list from all items, the end of list is indicated by a NULL
 */
gcry_sexp_t
gcry_sexp_vlist( const gcry_sexp_t a, ... )
{
  (void)a;
  /* NYI: Implementation should be quite easy with our new data
     representation. */
  BUG ();
  return NULL;
}


/****************
 * Append n to the list a
 * Returns: a new ist (which maybe a)
 */
gcry_sexp_t
gcry_sexp_append( const gcry_sexp_t a, const gcry_sexp_t n )
{
  (void)a;
  (void)n;
  /* NYI: Implementation should be quite easy with our new data
     representation. */
  BUG ();
  return NULL;
}

gcry_sexp_t
gcry_sexp_prepend( const gcry_sexp_t a, const gcry_sexp_t n )
{
  (void)a;
  (void)n;
  /* NYI: Implementation should be quite easy with our new data
     representation. */
  BUG ();
  return NULL;
}



/****************
 * Locate token in a list. The token must be the car of a sublist.
 * Returns: A new list with this sublist or NULL if not found.
 */
gcry_sexp_t
gcry_sexp_find_token( const gcry_sexp_t list, const char *tok, size_t toklen )
{
  const byte *p;
  DATALEN n;
  
  if ( !list )
    return NULL;

  if ( !toklen )
    toklen = strlen(tok);

  p = list->d;
  while ( *p != ST_STOP )
    {
      if ( *p == ST_OPEN && p[1] == ST_DATA ) 
        {
          const byte *head = p;

          p += 2;
          memcpy ( &n, p, sizeof n );
          p += sizeof n;
          if ( n == toklen && !memcmp( p, tok, toklen ) )
            { /* found it */
              gcry_sexp_t newlist;
              byte *d;
              int level = 1;

              /* Look for the end of the list.  */
              for ( p += n; level; p++ ) 
                {
                  if ( *p == ST_DATA )
                    {
			memcpy ( &n, ++p, sizeof n );
			p += sizeof n + n;
			p--; /* Compensate for later increment. */
		    }
                  else if ( *p == ST_OPEN ) 
                    {
                      level++;
		    }
                  else if ( *p == ST_CLOSE ) 
                    {
                      level--;
		    }
                  else if ( *p == ST_STOP ) 
                    {
                      BUG ();
		    }
		}
              n = p - head;

              newlist = gcry_malloc ( sizeof *newlist + n );
              if (!newlist)
                {
                  /* No way to return an error code, so we can only
                     return Not Found. */
                  return NULL;
                }
              d = newlist->d;
              memcpy ( d, head, n ); d += n;
              *d++ = ST_STOP;
              return normalize ( newlist );
	    }
          p += n;
	}
      else if ( *p == ST_DATA )
        {
          memcpy ( &n, ++p, sizeof n ); p += sizeof n;
          p += n;
	}
      else
        p++;
    }
  return NULL;
}

/****************
 * Return the length of the given list
 */
int
gcry_sexp_length( const gcry_sexp_t list )
{
    const byte *p;
    DATALEN n;
    int type;
    int length = 0;
    int level = 0;

    if ( !list )
	return 0;

    p = list->d;
    while ( (type=*p) != ST_STOP ) {
	p++;
	if ( type == ST_DATA ) {
	    memcpy ( &n, p, sizeof n );
	    p += sizeof n + n;
	    if ( level == 1 )
		length++;
	}
	else if ( type == ST_OPEN ) {
	    if ( level == 1 )
		length++;
	    level++;
	}
	else if ( type == ST_CLOSE ) {
	    level--;
	}
    }
    return length;
}


/* Return the internal lengths offset of LIST.  That is the size of
   the buffer from the first ST_OPEN, which is retruned at R_OFF, to
   the corresponding ST_CLOSE inclusive.  */
static size_t
get_internal_buffer (const gcry_sexp_t list, size_t *r_off)
{
  const unsigned char *p;
  DATALEN n;
  int type;
  int level = 0;
  
  *r_off = 0;
  if (list)
    {
      p = list->d;
      while ( (type=*p) != ST_STOP ) 
        {
          p++;
          if (type == ST_DATA) 
            {
              memcpy (&n, p, sizeof n);
              p += sizeof n + n;
            }
          else if (type == ST_OPEN)
            {
              if (!level)
                *r_off = (p-1) - list->d;
              level++;
            }
          else if ( type == ST_CLOSE )
            {
              level--;
              if (!level)
                return p - list->d;
            }
        }
    }
  return 0; /* Not a proper list.  */
}



/* Extract the CAR of the given list.  May return NULL for bad lists
   or memory failure.  */
gcry_sexp_t
gcry_sexp_nth( const gcry_sexp_t list, int number )
{
    const byte *p;
    DATALEN n;
    gcry_sexp_t newlist;
    byte *d;
    int level = 0;

    if ( !list || list->d[0] != ST_OPEN )
	return NULL;
    p = list->d;

    while ( number > 0 ) {
	p++;
	if ( *p == ST_DATA ) {
	    memcpy ( &n, ++p, sizeof n );
	    p += sizeof n + n;
	    p--;
	    if ( !level )
		number--;
	}
	else if ( *p == ST_OPEN ) {
	    level++;
	}
	else if ( *p == ST_CLOSE ) {
	    level--;
	    if ( !level )
		number--;
	}
	else if ( *p == ST_STOP ) {
	    return NULL;
	}
    }
    p++;

    if ( *p == ST_DATA ) {
	memcpy ( &n, p, sizeof n ); p += sizeof n;
	newlist = gcry_malloc ( sizeof *newlist + n + 1 );
        if (!newlist)
          return NULL;
	d = newlist->d;
	memcpy ( d, p, n ); d += n;
	*d++ = ST_STOP;
    }
    else if ( *p == ST_OPEN ) {
	const byte *head = p;

	level = 1;
	do {
	    p++;
	    if ( *p == ST_DATA ) {
		memcpy ( &n, ++p, sizeof n );
		p += sizeof n + n;
		p--;
	    }
	    else if ( *p == ST_OPEN ) {
		level++;
	    }
	    else if ( *p == ST_CLOSE ) {
		level--;
	    }
	    else if ( *p == ST_STOP ) {
		BUG ();
	    }
	} while ( level );
	n = p + 1 - head;

	newlist = gcry_malloc ( sizeof *newlist + n );
        if (!newlist)
          return NULL;
	d = newlist->d;
	memcpy ( d, head, n ); d += n;
	*d++ = ST_STOP;
    }
    else
	newlist = NULL;

    return normalize (newlist);
}

gcry_sexp_t
gcry_sexp_car( const gcry_sexp_t list )
{
    return gcry_sexp_nth ( list, 0 );
}


/* Helper to get data from the car.  The returned value is valid as
   long as the list is not modified. */
static const char *
sexp_nth_data (const gcry_sexp_t list, int number, size_t *datalen)
{
  const byte *p;
  DATALEN n;
  int level = 0;
  
  *datalen = 0;
  if ( !list ) 
    return NULL;

  p = list->d;
  if ( *p == ST_OPEN )
    p++;	     /* Yep, a list. */
  else if (number)
    return NULL;     /* Not a list but N > 0 requested. */

  /* Skip over N elements. */
  while ( number > 0 ) 
    {
      if ( *p == ST_DATA ) 
        {
          memcpy ( &n, ++p, sizeof n );
          p += sizeof n + n;
          p--;
          if ( !level )
            number--;
	}
      else if ( *p == ST_OPEN ) 
        {
          level++;
	}
      else if ( *p == ST_CLOSE ) 
        {
          level--;
          if ( !level )
            number--;
	}
      else if ( *p == ST_STOP ) 
        {
          return NULL;
	}
      p++;
    }

  /* If this is data, return it.  */
  if ( *p == ST_DATA )
    {
      memcpy ( &n, ++p, sizeof n );
      *datalen = n;
      return (const char*)p + sizeof n;
    }
  
  return NULL;
}


/* Get data from the car.  The returned value is valid as long as the
   list is not modified.  */
const char *
gcry_sexp_nth_data (const gcry_sexp_t list, int number, size_t *datalen )
{
  return sexp_nth_data (list, number, datalen);
}


/* Get a string from the car.  The returned value is a malloced string
   and needs to be freed by the caller.  */
char *
gcry_sexp_nth_string (const gcry_sexp_t list, int number)
{
  const char *s;
  size_t n;
  char *buf;

  s = sexp_nth_data (list, number, &n);
  if (!s || n < 1 || (n+1) < 1)
    return NULL;
  buf = gcry_malloc (n+1);
  if (!buf)
    return NULL;
  memcpy (buf, s, n);
  buf[n] = 0;
  return buf;
}

/*
 * Get a MPI from the car
 */
gcry_mpi_t
gcry_sexp_nth_mpi( gcry_sexp_t list, int number, int mpifmt )
{
  const char *s;
  size_t n;
  gcry_mpi_t a;

  if ( !mpifmt )
    mpifmt = GCRYMPI_FMT_STD;

  s = sexp_nth_data (list, number, &n);
  if (!s)
    return NULL;

  if ( gcry_mpi_scan ( &a, mpifmt, s, n, NULL ) )
    return NULL;

  return a;
}


/****************
 * Get the CDR
 */
gcry_sexp_t
gcry_sexp_cdr( const gcry_sexp_t list )
{
    const byte *p;
    const byte *head;
    DATALEN n;
    gcry_sexp_t newlist;
    byte *d;
    int level = 0;
    int skip = 1;

    if ( !list || list->d[0] != ST_OPEN )
	return NULL;
    p = list->d;

    while ( skip > 0 ) {
	p++;
	if ( *p == ST_DATA ) {
	    memcpy ( &n, ++p, sizeof n );
	    p += sizeof n + n;
	    p--;
	    if ( !level )
		skip--;
	}
	else if ( *p == ST_OPEN ) {
	    level++;
	}
	else if ( *p == ST_CLOSE ) {
	    level--;
	    if ( !level )
		skip--;
	}
	else if ( *p == ST_STOP ) {
	    return NULL;
	}
    }
    p++;

    head = p;
    level = 0;
    do {
	if ( *p == ST_DATA ) {
	    memcpy ( &n, ++p, sizeof n );
	    p += sizeof n + n;
	    p--;
	}
	else if ( *p == ST_OPEN ) {
	    level++;
	}
	else if ( *p == ST_CLOSE ) {
	    level--;
	}
	else if ( *p == ST_STOP ) {
	    return NULL;
	}
	p++;
    } while ( level );
    n = p - head;

    newlist = gcry_malloc ( sizeof *newlist + n + 2 );
    if (!newlist)
      return NULL;
    d = newlist->d;
    *d++ = ST_OPEN;
    memcpy ( d, head, n ); d += n;
    *d++ = ST_CLOSE;
    *d++ = ST_STOP;

    return normalize (newlist);
}

gcry_sexp_t
gcry_sexp_cadr ( const gcry_sexp_t list )
{
    gcry_sexp_t a, b;

    a = gcry_sexp_cdr ( list );
    b = gcry_sexp_car ( a );
    gcry_sexp_release ( a );
    return b;
}



static int
hextobyte( const byte *s )
{
    int c=0;

    if( *s >= '0' && *s <= '9' )
	c = 16 * (*s - '0');
    else if( *s >= 'A' && *s <= 'F' )
	c = 16 * (10 + *s - 'A');
    else if( *s >= 'a' && *s <= 'f' ) {
	c = 16 * (10 + *s - 'a');
    }
    s++;
    if( *s >= '0' && *s <= '9' )
	c += *s - '0';
    else if( *s >= 'A' && *s <= 'F' )
	c += 10 + *s - 'A';
    else if( *s >= 'a' && *s <= 'f' ) {
	c += 10 + *s - 'a';
    }
    return c;
}

struct make_space_ctx {
    gcry_sexp_t sexp;
    size_t allocated;
    byte *pos;
};

static gpg_err_code_t
make_space ( struct make_space_ctx *c, size_t n )
{
  size_t used = c->pos - c->sexp->d;
  
  if ( used + n + sizeof(DATALEN) + 1 >= c->allocated )
    {
      gcry_sexp_t newsexp;
      byte *newhead;
      size_t newsize;
      
      newsize = c->allocated + 2*(n+sizeof(DATALEN)+1);
      if (newsize <= c->allocated)
        return GPG_ERR_TOO_LARGE;
      newsexp = gcry_realloc ( c->sexp, sizeof *newsexp + newsize - 1);
      if (!newsexp)
        return gpg_err_code_from_errno (errno);
      c->allocated = newsize;
      newhead = newsexp->d;
      c->pos = newhead + used;
      c->sexp = newsexp;
    }
  return 0;
}


/* Unquote STRING of LENGTH and store it into BUF.  The surrounding
   quotes are must already be removed from STRING.  We assume that the
   quoted string is syntacillay correct.  */
static size_t
unquote_string (const char *string, size_t length, unsigned char *buf)
{
  int esc = 0;
  const unsigned char *s = (const unsigned char*)string;
  unsigned char *d = buf;
  size_t n = length;

  for (; n; n--, s++)
    {
      if (esc)
        {
          switch (*s)
            {
            case 'b':  *d++ = '\b'; break;
            case 't':  *d++ = '\t'; break;
            case 'v':  *d++ = '\v'; break;
            case 'n':  *d++ = '\n'; break;
            case 'f':  *d++ = '\f'; break;
            case 'r':  *d++ = '\r'; break;
            case '"':  *d++ = '\"'; break;
            case '\'': *d++ = '\''; break;
            case '\\': *d++ = '\\'; break;

            case '\r':  /* ignore CR[,LF] */
              if (n>1 && s[1] == '\n')
                {
                  s++; n--;
                }
              esc = 0;
              break;
              
            case '\n':  /* ignore LF[,CR] */
              if (n>1 && s[1] == '\r')
                {
                  s++; n--;
                }
              break;

            case 'x': /* hex value */
              if (n>2 && hexdigitp (s+1) && hexdigitp (s+2))
                {
                  s++; n--;
                  *d++ = xtoi_2 (s);
                  s++; n--;
                }
              break;

            default:
              if (n>2 && octdigitp (s) && octdigitp (s+1) && octdigitp (s+2))
                {
                  *d++ = (atoi_1 (s)*64) + (atoi_1 (s+1)*8) + atoi_1 (s+2);
                  s += 2;
                  n -= 2;
                }
              break;
	    }
          esc = 0;
        }
      else if( *s == '\\' )
        esc = 1;
      else
        *d++ = *s;
    } 

  return d - buf;
}

/****************
 * Scan the provided buffer and return the S expression in our internal
 * format.  Returns a newly allocated expression.  If erroff is not NULL and
 * a parsing error has occured, the offset into buffer will be returned.
 * If ARGFLAG is true, the function supports some printf like
 * expressions.
 *  These are:
 *	%m - MPI
 *	%s - string (no autoswitch to secure allocation)
 *	%d - integer stored as string (no autoswitch to secure allocation)
 *      %b - memory buffer; this takes _two_ arguments: an integer with the 
 *           length of the buffer and a pointer to the buffer.
 *      %S - Copy an gcry_sexp_t here.  The S-expression needs to be a
 *           regular one, starting with a parenthesis. 
 *           (no autoswitch to secure allocation)
 *  all other format elements are currently not defined and return an error.
 *  this includes the "%%" sequence becauce the percent sign is not an
 *  allowed character.
 * FIXME: We should find a way to store the secure-MPIs not in the string
 * but as reference to somewhere - this can help us to save huge amounts
 * of secure memory.  The problem is, that if only one element is secure, all
 * other elements are automagicaly copied to secure memory too, so the most
 * common operation gcry_sexp_cdr_mpi() will always return a secure MPI
 * regardless whether it is needed or not.
 */
static gcry_error_t
sexp_sscan (gcry_sexp_t *retsexp, size_t *erroff,
	    const char *buffer, size_t length, int argflag,
	    va_list arg_ptr, void **arg_list)
{
  gcry_err_code_t err = 0;
  static const char tokenchars[] =
    "abcdefghijklmnopqrstuvwxyz"
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "0123456789-./_:*+=";
  const char *p;
  size_t n;
  const char *digptr = NULL;
  const char *quoted = NULL;
  const char *tokenp = NULL;
  const char *hexfmt = NULL;
  const char *base64 = NULL;
  const char *disphint = NULL;
  const char *percent = NULL;
  int hexcount = 0;
  int quoted_esc = 0;
  int datalen = 0;
  size_t dummy_erroff;
  struct make_space_ctx c;
  int arg_counter = 0;
  int level = 0;

  if (!erroff)
    erroff = &dummy_erroff;

  /* Depending on wether ARG_LIST is non-zero or not, this macro gives
     us the next argument, either from the variable argument list as
     specified by ARG_PTR or from the argument array ARG_LIST.  */
#define ARG_NEXT(storage, type)                          \
  do                                                     \
    {                                                    \
      if (!arg_list)                                     \
	storage = va_arg (arg_ptr, type);                \
      else                                               \
	storage = *((type *) (arg_list[arg_counter++])); \
    }                                                    \
  while (0)

  /* The MAKE_SPACE macro is used before each store operation to
     ensure that the buffer is large enough.  It requires a global
     context named C and jumps out to the label LEAVE on error! It
     also sets ERROFF using the variables BUFFER and P.  */
#define MAKE_SPACE(n)  do {                                                \
                            gpg_err_code_t _ms_err = make_space (&c, (n)); \
                            if (_ms_err)                                   \
                              {                                            \
                                err = _ms_err;                             \
                                *erroff = p - buffer;                      \
                                goto leave;                                \
                              }                                            \
                       } while (0)

  /* The STORE_LEN macro is used to store the length N at buffer P. */
#define STORE_LEN(p,n) do {						   \
			    DATALEN ashort = (n);			   \
			    memcpy ( (p), &ashort, sizeof(ashort) );	   \
			    (p) += sizeof (ashort);			   \
			} while (0)

  /* We assume that the internal representation takes less memory than
     the provided one.  However, we add space for one extra datalen so
     that the code which does the ST_CLOSE can use MAKE_SPACE */
  c.allocated = length + sizeof(DATALEN);
  if (buffer && length && gcry_is_secure (buffer))
    c.sexp = gcry_malloc_secure (sizeof *c.sexp + c.allocated - 1);
  else
    c.sexp = gcry_malloc (sizeof *c.sexp + c.allocated - 1);
  if (!c.sexp)
    {
      err = gpg_err_code_from_errno (errno);
      *erroff = 0;
      goto leave;
    }
  c.pos = c.sexp->d;

  for (p = buffer, n = length; n; p++, n--)
    {
      if (tokenp && !hexfmt)
	{
	  if (strchr (tokenchars, *p))
	    continue;
	  else
	    {
	      datalen = p - tokenp;
	      MAKE_SPACE (datalen);
	      *c.pos++ = ST_DATA;
	      STORE_LEN (c.pos, datalen);
	      memcpy (c.pos, tokenp, datalen);
	      c.pos += datalen;
	      tokenp = NULL;
	    }
	}

      if (quoted)
	{
	  if (quoted_esc)
	    {
	      switch (*p)
		{
		case 'b': case 't': case 'v': case 'n': case 'f':
		case 'r': case '"': case '\'': case '\\':
		  quoted_esc = 0;
		  break;

		case '0': case '1': case '2': case '3': case '4':
		case '5': case '6': case '7':
		  if (!((n > 2)
                        && (p[1] >= '0') && (p[1] <= '7')
                        && (p[2] >= '0') && (p[2] <= '7')))
		    {
		      *erroff = p - buffer;
		      /* Invalid octal value.  */
		      err = GPG_ERR_SEXP_BAD_QUOTATION;
                      goto leave;
		    }
		  p += 2;
		  n -= 2;
		  quoted_esc = 0;
		  break;
		  
		case 'x':
		  if (!((n > 2) && hexdigitp (p+1) && hexdigitp (p+2)))
		    {
		      *erroff = p - buffer;
		      /* Invalid hex value.  */
		      err = GPG_ERR_SEXP_BAD_QUOTATION;
                      goto leave;
		    }
		  p += 2;
		  n -= 2;
		  quoted_esc = 0;
		  break;

		case '\r':
		  /* ignore CR[,LF] */
		  if (n && (p[1] == '\n'))
		    {
		      p++;
		      n--;
		    }
		  quoted_esc = 0;
		  break;

		case '\n':
		  /* ignore LF[,CR] */
		  if (n && (p[1] == '\r'))
		    {
		      p++;
		      n--;
		    }
		  quoted_esc = 0;
		  break;

		default:
		  *erroff = p - buffer;
		  /* Invalid quoted string escape.  */
		  err = GPG_ERR_SEXP_BAD_QUOTATION;
                  goto leave;
		}
	    }
	  else if (*p == '\\')
	    quoted_esc = 1;
	  else if (*p == '\"')
	    {
	      /* Keep it easy - we know that the unquoted string will
		 never be larger. */
	      unsigned char *save;
	      size_t len;
	      
	      quoted++; /* Skip leading quote.  */
	      MAKE_SPACE (p - quoted);
	      *c.pos++ = ST_DATA;
	      save = c.pos;
	      STORE_LEN (c.pos, 0); /* Will be fixed up later.  */
	      len = unquote_string (quoted, p - quoted, c.pos);
	      c.pos += len;
	      STORE_LEN (save, len);
	      quoted = NULL;
	    }
	}
      else if (hexfmt)
	{
	  if (isxdigit (*p))
	    hexcount++;
	  else if (*p == '#')
	    {
	      if ((hexcount & 1))
		{
		  *erroff = p - buffer;
		  err = GPG_ERR_SEXP_ODD_HEX_NUMBERS;
                  goto leave;
		}

	      datalen = hexcount / 2;
	      MAKE_SPACE (datalen);
	      *c.pos++ = ST_DATA;
	      STORE_LEN (c.pos, datalen);
	      for (hexfmt++; hexfmt < p; hexfmt++)
		{
		  if (whitespacep (hexfmt))
		    continue;
		  *c.pos++ = hextobyte ((const unsigned char*)hexfmt);
		  hexfmt++;
		}
	      hexfmt = NULL;
	    }
	  else if (!whitespacep (p))
	    {
	      *erroff = p - buffer;
	      err = GPG_ERR_SEXP_BAD_HEX_CHAR;
              goto leave;
	    }
	}
      else if (base64)
	{
	  if (*p == '|')
	    base64 = NULL;
	}
      else if (digptr)
	{
	  if (digitp (p))
	    ;
	  else if (*p == ':')
	    {
	      datalen = atoi (digptr); /* FIXME: check for overflow.  */
	      digptr = NULL;
	      if (datalen > n - 1)
		{
		  *erroff = p - buffer;
		  /* Buffer too short.  */
		  err = GPG_ERR_SEXP_STRING_TOO_LONG;
                  goto leave;
		}
	      /* Make a new list entry.  */
	      MAKE_SPACE (datalen);
	      *c.pos++ = ST_DATA;
	      STORE_LEN (c.pos, datalen);
	      memcpy (c.pos, p + 1, datalen);
	      c.pos += datalen;
	      n -= datalen;
	      p += datalen;
	    }
	  else if (*p == '\"')
	    {
	      digptr = NULL; /* We ignore the optional length.  */
	      quoted = p;
	      quoted_esc = 0;
	    }
	  else if (*p == '#')
	    {
	      digptr = NULL; /* We ignore the optional length.  */
	      hexfmt = p;
	      hexcount = 0;
	    }
	  else if (*p == '|')
	    {
	      digptr = NULL; /* We ignore the optional length.  */
	      base64 = p;
	    }
	  else
	    {
	      *erroff = p - buffer;
	      err = GPG_ERR_SEXP_INV_LEN_SPEC;
              goto leave;
	    }
	}
      else if (percent)
	{
	  if (*p == 'm')
	    {
	      /* Insert an MPI.  */
	      gcry_mpi_t m;
	      size_t nm = 0;

	      ARG_NEXT (m, gcry_mpi_t);
	      
	      if (gcry_mpi_print (GCRYMPI_FMT_STD, NULL, 0, &nm, m))
		BUG ();

	      MAKE_SPACE (nm);
	      if (!gcry_is_secure (c.sexp->d)
		  && gcry_mpi_get_flag ( m, GCRYMPI_FLAG_SECURE))
		{
		  /* We have to switch to secure allocation.  */
		  gcry_sexp_t newsexp;
		  byte *newhead;

		  newsexp = gcry_malloc_secure (sizeof *newsexp
                                                + c.allocated - 1);
                  if (!newsexp)
                    {
                      err = gpg_err_code_from_errno (errno);
                      goto leave;
                    }
		  newhead = newsexp->d;
		  memcpy (newhead, c.sexp->d, (c.pos - c.sexp->d));
		  c.pos = newhead + (c.pos - c.sexp->d);
		  gcry_free (c.sexp);
		  c.sexp = newsexp;
		}

	      *c.pos++ = ST_DATA;
	      STORE_LEN (c.pos, nm);
	      if (gcry_mpi_print (GCRYMPI_FMT_STD, c.pos, nm, &nm, m))
		BUG ();
	      c.pos += nm;
	    }
	  else if (*p == 's')
	    {
	      /* Insert an string.  */
	      const char *astr;
	      size_t alen;

	      ARG_NEXT (astr, const char *);
	      alen = strlen (astr);
	      
	      MAKE_SPACE (alen);
	      *c.pos++ = ST_DATA;
	      STORE_LEN (c.pos, alen);
	      memcpy (c.pos, astr, alen);
	      c.pos += alen;
	    }
	  else if (*p == 'b')
	    {
	      /* Insert a memory buffer.  */
	      const char *astr;
	      int alen;

	      ARG_NEXT (alen, int);
	      ARG_NEXT (astr, const char *);
	      
	      MAKE_SPACE (alen);
	      if (alen
                  && !gcry_is_secure (c.sexp->d)
		  && gcry_is_secure (astr))
              {
		  /* We have to switch to secure allocation.  */
		  gcry_sexp_t newsexp;
		  byte *newhead;

		  newsexp = gcry_malloc_secure (sizeof *newsexp
                                                + c.allocated - 1);
                  if (!newsexp)
                    {
                      err = gpg_err_code_from_errno (errno);
                      goto leave;
                    }
		  newhead = newsexp->d;
		  memcpy (newhead, c.sexp->d, (c.pos - c.sexp->d));
		  c.pos = newhead + (c.pos - c.sexp->d);
		  gcry_free (c.sexp);
		  c.sexp = newsexp;
		}

	      *c.pos++ = ST_DATA;
	      STORE_LEN (c.pos, alen);
	      memcpy (c.pos, astr, alen);
	      c.pos += alen;
	    }
	  else if (*p == 'd')
	    {
	      /* Insert an integer as string.  */
	      int aint;
	      size_t alen;
	      char buf[20];
	      
	      ARG_NEXT (aint, int);
	      sprintf (buf, "%d", aint);
	      alen = strlen (buf);
	      MAKE_SPACE (alen);
	      *c.pos++ = ST_DATA;
	      STORE_LEN (c.pos, alen);
	      memcpy (c.pos, buf, alen);
	      c.pos += alen;
	    }
	  else if (*p == 'S')
	    {
	      /* Insert a gcry_sexp_t.  */
	      gcry_sexp_t asexp;
	      size_t alen, aoff;

	      ARG_NEXT (asexp, gcry_sexp_t);
              alen = get_internal_buffer (asexp, &aoff);
              if (alen)
                {
                  MAKE_SPACE (alen);
                  memcpy (c.pos, asexp->d + aoff, alen);
                  c.pos += alen;
                }
	    }
	  else
	    {
	      *erroff = p - buffer;
	      /* Invalid format specifier.  */
	      err = GPG_ERR_SEXP_INV_LEN_SPEC;
              goto leave;
	    }
	  percent = NULL;
	}
      else if (*p == '(')
	{
	  if (disphint)
	    {
	      *erroff = p - buffer;
	      /* Open display hint.  */
	      err = GPG_ERR_SEXP_UNMATCHED_DH;
              goto leave;
	    }
	  MAKE_SPACE (0);
	  *c.pos++ = ST_OPEN;
	  level++;
	}
      else if (*p == ')')
	{
	  /* Walk up.  */
	  if (disphint)
	    {
	      *erroff = p - buffer;
	      /* Open display hint.  */
	      err = GPG_ERR_SEXP_UNMATCHED_DH;
              goto leave;
	    }
	  MAKE_SPACE (0);
	  *c.pos++ = ST_CLOSE;
	  level--;
	}
      else if (*p == '\"')
	{
	  quoted = p;
	  quoted_esc = 0;
	}
      else if (*p == '#')
	{
	  hexfmt = p;
	  hexcount = 0;
	}
      else if (*p == '|')
	base64 = p;
      else if (*p == '[')
	{
	  if (disphint)
	    {
	      *erroff = p - buffer;
	      /* Open display hint.  */
	      err = GPG_ERR_SEXP_NESTED_DH;
              goto leave;
	    }
	  disphint = p;
	}
      else if (*p == ']')
	{
	  if (!disphint)
	    {
	      *erroff = p - buffer;
	      /* Open display hint.  */
	      err = GPG_ERR_SEXP_UNMATCHED_DH;
              goto leave;
	    }
	  disphint = NULL;
	}
      else if (digitp (p))
	{
	  if (*p == '0')
	    {
	      /* A length may not begin with zero.  */
	      *erroff = p - buffer;
	      err = GPG_ERR_SEXP_ZERO_PREFIX;
              goto leave;
	    }
	  digptr = p;
	}
      else if (strchr (tokenchars, *p))
	tokenp = p;
      else if (whitespacep (p))
	;
      else if (*p == '{')
	{
	  /* fixme: handle rescanning: we can do this by saving our
	     current state and start over at p+1 -- Hmmm. At this
	     point here we are in a well defined state, so we don't
	     need to save it.  Great.  */
	  *erroff = p - buffer;
	  err = GPG_ERR_SEXP_UNEXPECTED_PUNC;
          goto leave;
	}
      else if (strchr ("&\\", *p))
	{
	  /* Reserved punctuation.  */
	  *erroff = p - buffer;
	  err = GPG_ERR_SEXP_UNEXPECTED_PUNC;
          goto leave;
	}
      else if (argflag && (*p == '%'))
	percent = p;
      else
	{
	  /* Bad or unavailable.  */
	  *erroff = p - buffer;
	  err = GPG_ERR_SEXP_BAD_CHARACTER;
          goto leave;
	}
    }
  MAKE_SPACE (0);
  *c.pos++ = ST_STOP;

  if (level && !err)
    err = GPG_ERR_SEXP_UNMATCHED_PAREN;

 leave:
  if (err)
    {
      /* Error -> deallocate.  */
      if (c.sexp)
        {
          /* Extra paranoid wipe on error. */
          if (gcry_is_secure (c.sexp))
            wipememory (c.sexp, sizeof (struct gcry_sexp) + c.allocated - 1);
          gcry_free (c.sexp);
        }
      /* This might be expected by existing code...  */
      *retsexp = NULL;
    }
  else
    *retsexp = normalize (c.sexp);

  return gcry_error (err);
#undef MAKE_SPACE
#undef STORE_LEN
}

gcry_error_t
gcry_sexp_build (gcry_sexp_t *retsexp, size_t *erroff, const char *format, ...)
{
  gcry_error_t rc;
  va_list arg_ptr;
  
  va_start (arg_ptr, format);
  rc = sexp_sscan (retsexp, erroff, format, strlen(format), 1,
		   arg_ptr, NULL);
  va_end (arg_ptr);
  
  return rc;
}


gcry_error_t
_gcry_sexp_vbuild (gcry_sexp_t *retsexp, size_t *erroff, 
                   const char *format, va_list arg_ptr)
{
  return sexp_sscan (retsexp, erroff, format, strlen(format), 1,
                     arg_ptr, NULL);
}

/* Like gcry_sexp_build, but uses an array instead of variable
   function arguments.  */
gcry_error_t
gcry_sexp_build_array (gcry_sexp_t *retsexp, size_t *erroff,
		       const char *format, void **arg_list)
{
  /* We don't need the va_list because it is controlled by the
     following flag, however we have to pass it but can't initialize
     it as there is no portable way to do so.  volatile is needed to
     suppress the compiler warning */
  volatile va_list dummy_arg_ptr;
  
  gcry_error_t rc;

  rc = sexp_sscan (retsexp, erroff, format, strlen(format), 1,
		   dummy_arg_ptr, arg_list);

  return rc;
}

gcry_error_t
gcry_sexp_sscan (gcry_sexp_t *retsexp, size_t *erroff,
		 const char *buffer, size_t length)
{
  /* We don't need the va_list because it is controlled by the
     following flag, however we have to pass it but can't initialize
     it as there is no portable way to do so.  volatile is needed to
     suppress the compiler warning */
  volatile va_list dummy_arg_ptr;

  return sexp_sscan (retsexp, erroff, buffer, length, 0,
		     dummy_arg_ptr, NULL);
}


/* Figure out a suitable encoding for BUFFER of LENGTH.
   Returns: 0 = Binary
            1 = String possible
            2 = Token possible
*/
static int
suitable_encoding (const unsigned char *buffer, size_t length)
{
  const unsigned char *s;
  int maybe_token = 1;

  if (!length)
    return 1;
  
  for (s=buffer; length; s++, length--)
    {
      if ( (*s < 0x20 || (*s >= 0x7f && *s <= 0xa0))
           && !strchr ("\b\t\v\n\f\r\"\'\\", *s))
        return 0; /*binary*/
      if ( maybe_token
           && !alphap (s) && !digitp (s)  && !strchr (TOKEN_SPECIALS, *s))
        maybe_token = 0;
    }
  s = buffer;
  if ( maybe_token && !digitp (s) )
    return 2;
  return 1;
}


static int
convert_to_hex (const unsigned char *src, size_t len, char *dest)
{
  int i;

  if (dest)
    {
      *dest++ = '#';
      for (i=0; i < len; i++, dest += 2 )
        sprintf (dest, "%02X", src[i]);
      *dest++ = '#';
    }
  return len*2+2;
}

static int
convert_to_string (const unsigned char *s, size_t len, char *dest)
{
  if (dest)
    {
      char *p = dest;
      *p++ = '\"';
      for (; len; len--, s++ )
        {
          switch (*s)
            {
            case '\b': *p++ = '\\'; *p++ = 'b';  break;
            case '\t': *p++ = '\\'; *p++ = 't';  break;
            case '\v': *p++ = '\\'; *p++ = 'v';  break;
            case '\n': *p++ = '\\'; *p++ = 'n';  break;
            case '\f': *p++ = '\\'; *p++ = 'f';  break;
            case '\r': *p++ = '\\'; *p++ = 'r';  break;
            case '\"': *p++ = '\\'; *p++ = '\"';  break;
            case '\'': *p++ = '\\'; *p++ = '\'';  break;
            case '\\': *p++ = '\\'; *p++ = '\\';  break;
            default: 
              if ( (*s < 0x20 || (*s >= 0x7f && *s <= 0xa0)))
                {
                  sprintf (p, "\\x%02x", *s); 
                  p += 4;
                }
              else
                *p++ = *s;
            }
        }
      *p++ = '\"';
      return p - dest;
    }
  else
    {
      int count = 2;
      for (; len; len--, s++ )
        {
          switch (*s)
            {
            case '\b': 
            case '\t': 
            case '\v': 
            case '\n': 
            case '\f': 
            case '\r': 
            case '\"':
            case '\'':
            case '\\': count += 2; break;
            default: 
              if ( (*s < 0x20 || (*s >= 0x7f && *s <= 0xa0)))
                count += 4;
              else
                count++;
            }
        }
      return count;
    }
}



static int
convert_to_token (const unsigned char *src, size_t len, char *dest)
{
  if (dest)
    memcpy (dest, src, len);
  return len;
}


/****************
 * Print SEXP to buffer using the MODE.  Returns the length of the
 * SEXP in buffer or 0 if the buffer is too short (We have at least an
 * empty list consisting of 2 bytes).  If a buffer of NULL is provided,
 * the required length is returned.
 */
size_t
gcry_sexp_sprint (const gcry_sexp_t list, int mode,
                  void *buffer, size_t maxlength )
{
  static unsigned char empty[3] = { ST_OPEN, ST_CLOSE, ST_STOP };
  const unsigned char *s;
  char *d;
  DATALEN n;
  char numbuf[20];
  size_t len = 0;
  int i, indent = 0;
  
  s = list? list->d : empty;
  d = buffer;
  while ( *s != ST_STOP )
    {
      switch ( *s )
        {
        case ST_OPEN:
          s++;
          if ( mode != GCRYSEXP_FMT_CANON )
            {
              if (indent)
                len++; 
              len += indent;
            }
          len++;
          if ( buffer ) 
            {
              if ( len >= maxlength )
                return 0;
              if ( mode != GCRYSEXP_FMT_CANON )
                {
                  if (indent)
                    *d++ = '\n'; 
                  for (i=0; i < indent; i++)
                    *d++ = ' ';
                }
              *d++ = '(';
	    }
          indent++;
          break;
        case ST_CLOSE:
          s++;
          len++;
          if ( buffer ) 
            {
              if ( len >= maxlength )
                return 0;
              *d++ = ')';
	    }
          indent--;
          if (*s != ST_OPEN && *s != ST_STOP && mode != GCRYSEXP_FMT_CANON)
            {
              len++;
              len += indent;
              if (buffer)
                {
                  if (len >= maxlength)
                    return 0;
                  *d++ = '\n';
                  for (i=0; i < indent; i++)
                    *d++ = ' ';
                }
            }
          break;
        case ST_DATA:
          s++;
          memcpy ( &n, s, sizeof n ); s += sizeof n;
          if (mode == GCRYSEXP_FMT_ADVANCED)
            {
              int type;
              size_t nn;

              switch ( (type=suitable_encoding (s, n)))
                {
                case 1: nn = convert_to_string (s, n, NULL); break;
                case 2: nn = convert_to_token (s, n, NULL); break;
                default: nn = convert_to_hex (s, n, NULL); break;
                }
              len += nn;
              if (buffer)
                {
                  if (len >= maxlength)
                    return 0;
                  switch (type)
                    {
                    case 1: convert_to_string (s, n, d); break;
                    case 2: convert_to_token (s, n, d); break;
                    default: convert_to_hex (s, n, d); break;
                    }
                  d += nn;
                }
              if (s[n] != ST_CLOSE)
                {
                  len++;
                  if (buffer)
                    {
                      if (len >= maxlength)
                        return 0;
                      *d++ = ' ';
                    }
                }
            }
          else
            {
              sprintf (numbuf, "%u:", (unsigned int)n );
              len += strlen (numbuf) + n;
              if ( buffer ) 
                {
                  if ( len >= maxlength )
		    return 0;
                  d = stpcpy ( d, numbuf );
                  memcpy ( d, s, n ); d += n;
                }
            }
          s += n;
          break;
        default:
          BUG ();
	}
    }
  if ( mode != GCRYSEXP_FMT_CANON )
    {
      len++;
      if (buffer)
        {
          if ( len >= maxlength )
            return 0;
          *d++ = '\n'; 
        }
    }
  if (buffer) 
    {
      if ( len >= maxlength )
        return 0;
      *d++ = 0; /* for convenience we make a C string */
    }
  else
    len++; /* we need one byte more for this */

  return len;
}


/* Scan a cannocial encoded buffer with implicit length values and
   return the actual length this S-expression uses.  For a valid S-Exp
   it should never return 0.  If LENGTH is not zero, the maximum
   length to scan is given - this can be used for syntax checks of
   data passed from outside. errorcode and erroff may both be passed as
   NULL.  */
size_t
gcry_sexp_canon_len (const unsigned char *buffer, size_t length, 
                     size_t *erroff, gcry_error_t *errcode)
{
  const unsigned char *p;
  const unsigned char *disphint = NULL;
  unsigned int datalen = 0;
  size_t dummy_erroff;
  gcry_error_t dummy_errcode;
  size_t count = 0;
  int level = 0;

  if (!erroff)
    erroff = &dummy_erroff;
  if (!errcode)
    errcode = &dummy_errcode;

  *errcode = gcry_error (GPG_ERR_NO_ERROR);
  *erroff = 0;
  if (!buffer)
    return 0;
  if (*buffer != '(')
    {
      *errcode = gcry_error (GPG_ERR_SEXP_NOT_CANONICAL);
      return 0;
    }

  for (p=buffer; ; p++, count++ )
    {
      if (length && count >= length)
        {
          *erroff = count;
          *errcode = gcry_error (GPG_ERR_SEXP_STRING_TOO_LONG);
          return 0;
        }
      
      if (datalen)
        {
          if (*p == ':')
            {
              if (length && (count+datalen) >= length)
                {
                  *erroff = count;
                  *errcode = gcry_error (GPG_ERR_SEXP_STRING_TOO_LONG);
                  return 0;
                }
              count += datalen;
              p += datalen;
              datalen = 0;
	    }
          else if (digitp(p))
            datalen = datalen*10 + atoi_1(p);
          else 
            {
              *erroff = count;
              *errcode = gcry_error (GPG_ERR_SEXP_INV_LEN_SPEC);
              return 0;
	    }
	}
      else if (*p == '(')
        {
          if (disphint)
            {
              *erroff = count;
              *errcode = gcry_error (GPG_ERR_SEXP_UNMATCHED_DH);
              return 0;
	    }
          level++;
	}
      else if (*p == ')')
        { /* walk up */
          if (!level)
            {
              *erroff = count;
              *errcode = gcry_error (GPG_ERR_SEXP_UNMATCHED_PAREN);
              return 0;
	    }
          if (disphint)
            {
              *erroff = count;
              *errcode = gcry_error (GPG_ERR_SEXP_UNMATCHED_DH);
              return 0;
	    }
          if (!--level)
            return ++count; /* ready */
	}
      else if (*p == '[')
        {
          if (disphint) 
            {
              *erroff = count;
              *errcode = gcry_error (GPG_ERR_SEXP_NESTED_DH);
              return 0;
            }
          disphint = p;
	}
      else if (*p == ']')
        {
          if ( !disphint ) 
            {
              *erroff = count;
              *errcode = gcry_error (GPG_ERR_SEXP_UNMATCHED_DH);
              return 0;
	    }
          disphint = NULL;
	}
      else if (digitp (p) )
        {
          if (*p == '0')
            { 
              *erroff = count;
              *errcode = gcry_error (GPG_ERR_SEXP_ZERO_PREFIX);
              return 0;
	    }
          datalen = atoi_1 (p);
	}
      else if (*p == '&' || *p == '\\')
        {
          *erroff = count;
          *errcode = gcry_error (GPG_ERR_SEXP_UNEXPECTED_PUNC);
          return 0;
	}
      else
        { 
          *erroff = count;
          *errcode = gcry_error (GPG_ERR_SEXP_BAD_CHARACTER);
          return 0;
	}
    }
}
