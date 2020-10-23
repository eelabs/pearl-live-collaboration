GRADLE:=./gradlew
GRADLEFLAGS:=--warning-mode all
BUILD:=echo -n | $(GRADLE)
BUILDFLAGS:=$(GRADLEFLAGS)
GRADLE_BOOTSTRAP:=echo -n | $(firstword $(wildcard $(GRADLE) $(shell which gradle)))

.PHONY: all
## Builds and verifies the project.
all: build

.PHONY: continuous
## Builds and tests continuously.
continuous:
	$(BUILD) $(BUILDFLAGS) --continuous test

.PHONY: build
## Builds and verifies the project.
build: $(GRADLE)
	$(BUILD) $(BUILDFLAGS) $@

.PHONY: clean
clean:: $(GRADLE)
	$(BUILD) $(BUILDFLAGS) $@

.PHONY: wrapper
## Generates the wrapper.
wrapper: $(GRADLE)
$(GRADLE):
	$(GRADLE_BOOTSTRAP) wrapper

-include .makehelp/include/makehelp/Help.mk

ifeq (help, $(filter help,$(MAKECMDGOALS)))
.makehelp/include/makehelp/Help.mk:
	git clone --depth=1 https://github.com/christianhujer/makehelp.git .makehelp
endif

-include ~/.User.mk
-include .User.mk
