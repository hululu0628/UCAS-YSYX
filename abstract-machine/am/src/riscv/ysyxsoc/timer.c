#include <am.h>
#include "ysyxsoc.h"

void __am_timer_init() {
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
	uint32_t us_h, us_l;
	us_h = inl(CLINT_BASE + CLINT_HTIME);
	us_l = inl(CLINT_BASE + CLINT_LTIME);
	uptime->us = ((uint64_t)us_h + (uint64_t)us_l) / CONFIG_SIM_FREQ_M;
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}
