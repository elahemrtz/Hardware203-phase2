package hardwar.branch.prediction.judged.SAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAg implements BranchPredictor {
    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table

    public SAg() {
        this(4, 2, 8, 4);
    }

    public SAg(int BHRSize, int SCSize, int branchInstructionSize, int KSize) {
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;

        // Initialize the PSBHR with the given BHR and K size
        PSBHR = new RegisterBank(BHRSize, KSize);

        // Initialize the PHT with a size of 2^BHRSize and each entry having a saturating counter of size "SCSize"
        int phtSize = (int) Math.pow(2, BHRSize);
        int blockSize = SCSize;
        PHT = new Cache<>(phtSize, blockSize);

        // Initialize the SC register with the given size
        SC = new ShiftRegister(SCSize);
    }

    @Override
    public BranchResult predict(BranchInstruction instruction) {
        // Read the branch address
        Bit[] branchAddress = instruction.getBranchAddress();

        // Get the register bank address line based on the branch address
        Bit[] rbAddressLine = getRBAddressLine(branchAddress);

        // Read the associated block with the RB address line from the PHT
        Bit[] phtEntry = PHT.read(rbAddressLine);

        // Load the read block from the cache into the SC register
        SC.setBits(phtEntry);

        // Return the MSB of the read block or SC register
        return SC.getBits()[0] == Bit.ONE ? BranchResult.TAKEN : BranchResult.NOT_TAKEN;
    }

    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        // Read the branch address
        Bit[] branchAddress = branchInstruction.getBranchAddress();

        // Get the register bank address line based on the branch address
        Bit[] rbAddressLine = getRBAddressLine(branchAddress);

        // Read the associated block with the RB address line from the PHT
        Bit[] phtEntry = PHT.read(rbAddressLine);

        // Pass the SC register bits to a saturating counter
        SaturatingCounter counter = new SaturatingCounter(SC.getLength());
        counter.setBits(SC.getBits());

        // Update the saturating counter based on the actual branch result
        if (actual == BranchResult.TAKEN) {
            counter.increment();
        } else {
            counter.decrement();
        }

        // Save the updated value into the cache via RB address line
        Bit[] updatedValue = counter.getBits();
        PHT.write(rbAddressLine, updatedValue);

        // Update the PSBHR with the actual branch result
        PSBHR.setBits(rbAddressLine, actual == BranchResult.TAKEN ? Bit.ONE : Bit.ZERO);
    }

    private Bit[] getRBAddressLine(Bit[] branchAddress) {
        // Hash the branch address
        return hash(branchAddress);
    }

    /**
     * Hash N bits to a K bit value.
     *
     * @param bits program counter
     * @return hash value of first M bits of `bits` in K bits
     */
    private Bit[] hash(Bit[] bits) {
        Bit[] hash = new Bit[KSize];

        // XOR the first M bits of the PC to produce the hash
        for (int i = 0; i < branchInstructionSize; i++) {
            int j = i % KSize;
            if (hash[j] == null) {
                hash[j] = bits[i];
            } else {
                Bit xorProduce = hash[j].getValue() ^ bits[i].getValue() ? Bit.ONE : Bit.ZERO;
                hash[j] = xorProduce;
            }
        }
        return hash;
    }

    @Override
    public String monitor() {
        return "SAg predictor snapshot:\n" + PSBHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
