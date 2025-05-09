
CONFIG_DIR = $(NPC_HOME)/config

# set environtment for kconfig
# ensure that the directory in these path already exists
KCONFIG_ENV = env KCONFIG_AUTOHEADER=$(CONFIG_DIR)/autoconf.h KCONFIG_AUTOCONFIG=$(CONFIG_DIR)/auto.conf

menuconfig:
	kconfig-mconf $(CONFIG_DIR)/Kconfig
	$(KCONFIG_ENV) kconfig-conf --silentoldconfig $(CONFIG_DIR)/Kconfig
