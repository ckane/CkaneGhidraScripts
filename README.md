# Ghidra Scripts

This is a repository of some scripts I am using for my research and experiments.

## JsonBlocks.java

This script will dump one program's functions &amp; basic blocks, with instructions and Pcode, into a Json file named
<tt>output.json</tt>, in the current working directory.

Example execution:

```bash
/path/to/ghidra/support/analyzeHeadless /projects/path ProjectName -import /full/path/to/program.exe -postScript JsonBlocks.java
```

```bash
/path/to/ghidra/support/analyzeHeadless /projects/path ProjectName -process program.exe -postScript JsonBlocks.java
```
