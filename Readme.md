# Explore Diplomacy using Rocket-Chip  Interrupts

This project uses [playground](https://github.com/morphingmachines/playground.git) as a library. `playground` and `this project` directories should be at the same level, as shown below.
```
  workspace
  |-- playground
  |-- rocket-chip-interrupt-explorer
```
Make sure that you have a working [playground](https://github.com/morphingmachines/playground.git) project before proceeding further. And donot rename/modify `playground` directory structure.


## Generating Verilog

Verilog code can be generated from Chisel by using the `rtl` Makefile target.

```sh
make rtl
```

The output verilog files are generated in the `./generated_sv_dir` directory. This also generates a `graphml` file that visualizes the diplomacy graph of different components in the system. To view `graphml` file, as shown below, use [yEd](https://askubuntu.com/a/504178). 

### Run tests

```sh
make test
```

The output VCD files are dumped in the `./test_run_dir` directory.

More targets can be listed by running `make`.

## Diplomacy Learning Resources

- [Rocket-chip-read](https://github.com/morphingmachines/rocket-chip-read.git)
- [Diplomatic Adder](https://github.com/morphingmachines/diplomacy-example.git)

