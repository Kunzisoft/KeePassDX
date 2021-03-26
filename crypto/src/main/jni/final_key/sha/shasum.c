/*
---------------------------------------------------------------------------
Copyright (c) 1998-2010, Brian Gladman, Worcester, UK. All rights reserved.

The redistribution and use of this software (with or without changes)
is allowed without the payment of fees or royalties provided that:

  source code distributions include the above copyright notice, this
  list of conditions and the following disclaimer;

  binary distributions include the above copyright notice, this list
  of conditions and the following disclaimer in their documentation.

This software is provided 'as is' with no explicit or implied warranties
in respect of its operation, including, but not limited to, correctness
and fitness for purpose.
---------------------------------------------------------------------------
Issue Date: 20/12/2007
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "sha2.h"

#define BUF_SIZE	16384

int main(int argc, char *argv[])
{	FILE			*inf;
	sha256_ctx		ctx[1];
	unsigned char	buf[BUF_SIZE], hval[SHA256_DIGEST_SIZE];
	int				i, len, is_console;

	if(argc != 2)
	{
		printf("\nusage: shasum filename\n");
		exit(0);
	}

	if(is_console = (!strcmp(argv[1], "con") || !strcmp(argv[1], "CON")))
	{
		if(!(inf = fopen(argv[1], "r")))
		{
			printf("\n%s not found\n", argv[1]);
			exit(0);
		}
	}
	else if(!(inf = fopen(argv[1], "rb")))
	{
		printf("\n%s not found\n", argv[1]);
		exit(0);
	}

	sha256_begin(ctx);
	do
	{
		len = (int)fread(buf, 1, BUF_SIZE, inf);
		i = len;
		if(is_console)
		{
			i = 0;
			while(i < len && buf[i] != '\x1a')
				++i;
		}
		if(i)
			sha256_hash(buf, i, ctx);
	}
	while
		(len && i == len);

	fclose(inf);
	sha256_end(hval, ctx);

	printf("\n");
	for(i = 0; i < SHA256_DIGEST_SIZE; ++i)
		printf("%02x", hval[i]);
	printf("\n");

	return 0;
}
