#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  panic("Not implemented");
}

int vsprintf(char *out, const char *fmt, va_list ap)
{
	char buf[64];
	while(*fmt)
	{
		if(*fmt == '%')
		{
			fmt++; // get the conversion character
			switch(*fmt)
			{
				case 's':
				{
					char *p = va_arg(ap, char *);
					strcpy(out, p);
					out += strlen(p);
					fmt++;
					break;
				}
				case 'd':
				{
					int num = va_arg(ap, int);
					int len = 0;
					char temp[64];
					char *p = temp;
					char *q = buf;
					// itoa
					if(num < 0)
					{
						*q++ = '-';
						num = -num;
					}
					do
					{
						*p = num % 10 + '0';
						p++;
						num /= 10;
						len++;
					} while(num);
					*p = '\0';
					// reverse
					p--;
					while(len--)
						*q++ = *p--;
					*q = '\0';
					strcpy(out, buf);
					out += strlen(buf);

					fmt++;
					break;
				}
				default:
					panic("Not implemented");
			}
		}
		else
		{
			*out = *fmt;
			out++;fmt++;
		}
	}
	*out = '\0';
	return strlen(out);
}

int sprintf(char *out, const char *fmt, ...)
{
	int ret;
	va_list ap;
	va_start(ap,fmt);
	ret = vsprintf(out,fmt,ap);
	va_end(ap);
	return ret;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
