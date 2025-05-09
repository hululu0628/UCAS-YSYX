
CONFIG_DIR = $(NPC_HOME)/config

# set environtment for kconfig
# ensure that the directory in these path already exists
KCONFIG_ENV = env KCONFIG_AUTOHEADER=$(CONFIG_DIR)/include/autoconf.h KCONFIG_AUTOCONFIG=$(CONFIG_DIR)/include/auto.conf

menuconfig:
	@mkdir -p $(CONFIG_DIR)/include/config
	@mkdir -p $(CONFIG_DIR)/include/generated
	kconfig-mconf $(CONFIG_DIR)/Kconfig
	kconfig-conf --silentoldconfig $(CONFIG_DIR)/Kconfig
