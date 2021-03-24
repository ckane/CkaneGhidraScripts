import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.Listing;
import ghidra.program.model.listing.CodeUnitIterator;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.address.AddressSetView;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.address.AddressRangeIterator;
import ghidra.program.model.address.AddressRange;
import ghidra.program.model.listing.InstructionIterator;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.pcode.PcodeOp;
import java.util.Vector;
import java.io.PrintStream;

/*
 * Program
 *  List of Functions
 *   Function [begin addr]
 *    List of basic blocks
 *     Basic Blocks
 *      Basic Block [begin addr, length in bytes]
 *       List of Machine instrs
 *        Machine Instr [str of instr, length in bytes]
 *         List of PcodeOps
 *          PcodeOp [str of Pcode, pcodeop int]
 */

public class JsonBlocks extends GhidraScript {
    public class CkPcodeOp {
        public String PcodeStr;
        public int PcodeOp;
        public CkPcodeOp() {};
        public void dumpJson(PrintStream o) {
            o.printf("{\"pcode\":\"%s\",\"opid\":%d}", PcodeStr, PcodeOp);
        };
    }
    public class CkMachInstr {
        public String MachInstrStr;
        public int length;
        public Vector<CkPcodeOp> pcode_ops;
        public CkMachInstr() {
            pcode_ops = new Vector<CkPcodeOp>(0);
        };
        public void dumpJson(PrintStream o) {
            o.printf("{\"instruction\":\"%s\",\"pcodeops\":[", MachInstrStr);
            boolean first = true;
            for(CkPcodeOp ckpo : pcode_ops) {
                if(!first) {
                    o.print(",");
                };
                first = false;
                ckpo.dumpJson(o);
            };
            o.printf("]}");
        };
    }
    public class CkBasicBlock {
        public long start_addr, length;
        public Vector<CkMachInstr> machInstrs;
        public CkBasicBlock() {
            machInstrs = new Vector<CkMachInstr>(0);
        };
        public void dumpJson(PrintStream o) {
            o.printf("{\"address\":%d,\"instructions\":[", start_addr);
            boolean first = true;
            for(CkMachInstr ckmi : machInstrs) {
                if(!first) {
                    o.print(",");
                };
                first = false;
                ckmi.dumpJson(o);
            };
            o.printf("]}");
        };
    }
    public class CkFunction {
        public long start_addr;
        public String name;
        public Vector<CkBasicBlock> blocks;
        public CkFunction() {
            blocks = new Vector<CkBasicBlock>(0);
        };
        public void dumpJson(PrintStream o) {
            o.printf("{\"address\":%d,\"name\":\"%s\",\"blocks\":[", start_addr, name);
            boolean first = true;
            for(CkBasicBlock ckbb : blocks) {
                if(!first) {
                    o.print(",");
                };
                first = false;
                ckbb.dumpJson(o);
            };
            o.printf("]}");
        };
    }
    public class CkProgram {
        public Vector<CkFunction> funcs;
        public CkProgram() {
            funcs = new Vector<CkFunction>(0);
        };
        public void dumpJson(PrintStream o) {
            o.print("{\"functions\":[");
            boolean first = true;
            for(CkFunction ckf : funcs) {
                if(!first) {
                    o.print(",");
                };
                first = false;
                ckf.dumpJson(o);
            };
            o.print("]}");
        };
    }
    public void run() throws Exception {
        int i = 0;
        CkProgram my_prog = new CkProgram();
        Listing ll = currentProgram.getListing();
        CodeUnitIterator cui = ll.getCodeUnits(true);
        FunctionIterator fni = currentProgram.getFunctionManager().getFunctions(true);

        for(Function fn : fni) {
            int j = 0;
            CkFunction ck_fn = new CkFunction();
            ck_fn.start_addr = fn.getEntryPoint().getOffset();
            ck_fn.name = fn.getName();
            AddressSetView asv = fn.getBody();
            AddressRangeIterator ari = asv.getAddressRanges();
            for(AddressRange ar : ari) {
                CkBasicBlock ck_bb = new CkBasicBlock();
                System.out.println("------------------------------------------------");
                AddressSet as_instr = new AddressSet(ar);
                InstructionIterator ii = ll.getInstructions(as_instr, true);
                for(Instruction instr : ii) {
                    CkMachInstr ck_instr = new CkMachInstr();
                    ck_instr.MachInstrStr = instr.toString();
                    System.out.println("x86asm: " + ck_instr.MachInstrStr);
                    PcodeOp p[] = instr.getPcode();
                    ck_bb.machInstrs.addElement(ck_instr);
                    for(PcodeOp p_op : p) {
                        CkPcodeOp ck_pcode = new CkPcodeOp();
                        ck_pcode.PcodeStr = p_op.toString();
                        ck_pcode.PcodeOp = p_op.getOpcode();
                        ck_instr.pcode_ops.addElement(ck_pcode);
                        //System.out.println("(" + Integer.toString(p_op.getOpcode()) + ") " + p_op.toString());
                        System.out.println("PCode : " + p_op.toString());
                        if(p_op.getOpcode() == PcodeOp.BRANCH || p_op.getOpcode() == PcodeOp.BRANCHIND ||
                                p_op.getOpcode() == PcodeOp.CBRANCH || p_op.getOpcode() == PcodeOp.RETURN) {
                            ck_fn.blocks.addElement(ck_bb);
                            ck_bb = new CkBasicBlock();
                            System.out.println("------------------------------------------------");
                        }
                    }
                }
                ck_fn.blocks.addElement(ck_bb);
                j++;
            }
            System.out.println(Integer.toString(i) + " count: " + Integer.toString(j));
            i++;
            my_prog.funcs.addElement(ck_fn);
        }

        System.out.println(Integer.toString(i));
        my_prog.dumpJson(new PrintStream("output.json"));
    }
}
