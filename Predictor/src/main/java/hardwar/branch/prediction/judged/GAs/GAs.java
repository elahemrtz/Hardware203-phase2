package hardwar.branch.prediction.judged.GAs;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class GAs implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final HashMode hashMode;
    private final ShiftRegister SC; // saturating counter register
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PSPHT; // Per Set Predication History Table

    public GAs() {
        this(4, 2, 8, 4, HashMode.XOR);
    }

    public GAs(int BHRSize, int SCSize, int branchInstructionSize, int KSize, HashMode hashMode) {
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;
        this.hashMode = hashMode;

        // Initialize the BHR register with the given size and no default value
        BHR = new ShiftRegister(BHRSize);

        // Initialize the PSPHT with K bit as PHT selector and 2^BHRSize rows as each PHT entry
        // number, and SCSize as the block size
        PSPHT = new Cache<>((int) Math.pow(2, BHRSize), getDefaultBlock(SCSize));

        // Initialize the saturating counter
        SC = new ShiftRegister(SCSize);
    }

    /**
     * Predicts the result of a branch instruction based on the global branch history and hash value of
     * branch instruction address.
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // Get the branch address bits
        Bit[] branchAddress = branchInstruction.getBits();

        // Get the cache entry using the branch address and BHR
        Bit[] cacheEntry = getCacheEntry(branchAddress);

        // Retrieve the corresponding cache block from the PSPHT
        Bit[] cacheBlock = PSPHT.read(cacheEntry);

        // Use the last bit of the cache block as the prediction
        BranchResult prediction = cacheBlock[cacheBlock.length - 1].equals(Bit.ZERO)
                ? BranchResult.NOT_TAKEN
                : BranchResult.TAKEN;

        return prediction;
    }

    /**
     * Updates the value in the cache based on the actual branch result.
     *
     * @param branchInstruction the branch instruction
     * @param actual            the actual result of the branch (taken or not taken)
     */
    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        // Get the branch address bits
        Bit[] branchAddress = branchInstruction.getBits();

        // Get the cache entry using the branch address and BHR
        Bit[] cacheEntry = getCacheEntry(branchAddress);

        // Retrieve the corresponding cache block from the PSPHT
        Bit[] cacheBlock = PSPHT.read(cacheEntry);

        // Update the prediction in the cache block based on the actual result
        Bit[] updatedBlock = Arrays.copyOf(cacheBlock, cacheBlock.length);
        updatedBlock[updatedBlock.length - 1] = actual.equals(BranchResult.TAKEN) ? Bit.ONE : Bit.ZERO;

        // Write the updated cache block back to the PSPHT
        PSPHT.write(cacheEntry, updatedBlock);
    }

    /**
     * @return snapshot of caches and registers content
     */
    public String monitor() {
        return "GAs predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PSPHT.monitor();
    }

    /**
     * Concatenates the branch address hash and BHR to retrieve the desired address.
     *
     * @param branchAddress program counter
     * @return concatenated value of the branch address hash and BHR
     */
    private Bit[] getCacheEntry(Bit[] branchAddress) {
        // Hash the branch address
        Bit[] hashKSize = CombinationalLogic.hash(branchAddress, KSize, hashMode);

        // Concatenate the Hash bits with the BHR bits
        Bit[] bhrBits = BHR.read();
        Bit[] cacheEntry = new Bit[hashKSize.length + bhrBits.length];
        System.arraycopy(hashKSize, 0, cacheEntry, 0, hashKSize.length);
        System.arraycopy(bhrBits, 0, cacheEntry, hashKSize.length, bhrBits.length);

        return cacheEntry;
    }

    private Bit[] getDefaultBlock(int blockSize) {
        Bit[] defaultBlock = new Bit[blockSize];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }
}
