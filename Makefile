# Replace 'adder' with your %PROJECT-NAME%
project = explorer

# Toolchains and tools
MILL = ./../playground/mill

-include ../playground/Makefile.include

# Targets
rtl:## Generates Verilog code from Chisel sources (output to ./generated_sv_dir)
	$(MILL) $(project).runMain explorer.interruptsMain example1

check: test
.PHONY: test
test:## Run Chisel tests
	$(MILL) $(project).test.testOnly explorer.DummySpec
	@echo "The VCD file is generated in ./test_run_dir/testname directories."

