# Replace 'adder' with your %PROJECT-NAME%
project = explorer

# Toolchains and tools
MILL = ./mill


# Targets
rtl:## Generates Verilog code from Chisel sources (output to ./generated_sv_dir)
	$(MILL) $(project).runMain explorer.interruptsMain example1

check: test
.PHONY: test
test:## Run Chisel tests
	$(MILL) $(project).test.testOnly explorer.DummySpec
	@echo "If using WriteVcdAnnotation in your tests, the VCD files are generated in ./test_run_dir/testname directories."

.PHONY: lint
lint: ## Formats code using scalafmt and scalafix
	$(MILL) $(project).fix
	$(MILL) $(project).reformat

.PHONY: lint-test
lint-test: ## Formats test code using scalafmt and scalafix
	$(MILL) $(project).test.fix
	$(MILL) $(project).test.reformat



.PHONY: console
console: ## Start a scala console within this project
	$(MILL) -i $(project).console

.PHONY: clean
clean:   ## Clean all generated files
	$(MILL) clean
	@rm -rf test_run_dir generated_sv_dir
	@rm -rf out

.PHONY: cleanall
cleanall: clean  ## Clean all downloaded dependencies and cache
	@rm -rf project/.bloop
	@rm -rf project/project
	@rm -rf project/target
	@rm -rf .bloop .bsp .metals 

.PHONY: help
help:
	@echo "Makefile targets:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = "[:##]"}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$4}'
	@echo ""

.DEFAULT_GOAL := help
