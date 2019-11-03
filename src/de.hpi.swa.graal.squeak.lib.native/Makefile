QUIETLY$(MX_VERBOSE) = @
LIBDIR=../../lib

TARGET = SqueakFFIPrims

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
	LIBFLAGS = -shared
	EXTENSION = so
endif
ifeq ($(UNAME_S),Darwin)
	LIBFLAGS = -dynamiclib
	EXTENSION = dylib
endif

$(LIBDIR)/$(TARGET).$(EXTENSION): src/$(TARGET).c
	$(CC) -fPIC $(LIBFLAGS) src/$(TARGET).c -o $(LIBDIR)/$(TARGET).$(EXTENSION)

.PHONY: clean
clean:
	rm -f $(LIBDIR)/$(TARGET).$(EXTENSION)
