/**
 * replace rt-thread-am/bsp/abstract-machine/src/context.c
 */

#include <am.h>
#include <klib.h>
#include <rtthread.h>

typedef struct {
	rt_ubase_t from;
	rt_ubase_t to;
} FromTo_t;

static Context* ev_handler(Event e, Context *c) {
	rt_thread_t current_thread = rt_thread_self();
	rt_ubase_t data = current_thread->user_data;
	switch (e.event) {
		case EVENT_YIELD: 
			if(((FromTo_t *)data)->from != 0)
				*((Context **)((FromTo_t *)data)->from) = c;
			if(((FromTo_t *)data)->to != 0)
				c = *((Context **)((FromTo_t *)data)->to);
			break;
		case EVENT_IRQ_TIMER: break;
		default: printf("Unhandled event ID = %d\n", e.event); assert(0);
	}
	return c;
}

void __am_cte_init() {
  cte_init(ev_handler);
}

void rt_hw_context_switch_to(rt_ubase_t to) {
	FromTo_t temp = {.from = 0, .to = 0};
	rt_ubase_t pcb_user_data_temp;
	temp.from = 0;
	temp.to = to;

	rt_thread_t current_thread = rt_thread_self();
	pcb_user_data_temp = current_thread->user_data;
	current_thread->user_data = (rt_ubase_t)&temp;
	yield();
	current_thread->user_data = pcb_user_data_temp;
}

void rt_hw_context_switch(rt_ubase_t from, rt_ubase_t to) {
	FromTo_t temp = {.from = 0, .to = 0};
	rt_ubase_t pcb_user_data_temp;
	temp.from = from;
	temp.to = to;

	rt_thread_t current_thread = rt_thread_self();
	pcb_user_data_temp = current_thread->user_data;
	current_thread->user_data = (rt_ubase_t)&temp;
	yield();
	current_thread->user_data = pcb_user_data_temp;
}

void rt_hw_context_switch_interrupt(void *context, rt_ubase_t from, rt_ubase_t to, struct rt_thread *to_thread) {
  assert(0);
}

void rt_hw_tentry_wrapper(void *tentry, void *parameter, void *texit)
{
	void (*entry)(void *) = (void (*)(void *))tentry;
	void (*exit)(void) = (void (*)(void))texit;
	entry(parameter);
	exit();
}

rt_uint8_t *rt_hw_stack_init(void *tentry, void *parameter, rt_uint8_t *stack_addr, void *texit) {
	uintptr_t addr_aligned = (uintptr_t)stack_addr & ~(sizeof(uintptr_t) - 1);
	Context *c = (Context *)(addr_aligned - sizeof(Context));
	c->mepc = (uintptr_t)rt_hw_tentry_wrapper;
	c->mstatus = 0x21800; // [hululu.PA3]: initialize mstatus for difftest (rv32)
	c->gpr[REG_A0] = (uintptr_t)tentry;
	c->gpr[REG_A1] = (uintptr_t)parameter;
	c->gpr[REG_A2] = (uintptr_t)texit;
	return (rt_uint8_t *)c;
}
