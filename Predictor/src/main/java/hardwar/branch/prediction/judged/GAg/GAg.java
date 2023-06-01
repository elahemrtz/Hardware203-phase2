package hardwar.branch.prediction.judged.GAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.Cache;
import hardwar.branch.prediction.shared.devices.ShiftRegister;

import java.util.Arrays;

public class GAg implements BranchPredictor {
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table
    private final ShiftRegister SC; // saturated counter register

    public GAg() {
        this(4, 2);
    }

    /**
     * Creates a new GAg predictor with the given BHR register size and initializes the BHR and PHT.
     *
     * @param BHRSize the size of the BHR register
     * @param SCSize  the size of the register which hold the saturating counter value and the cache block size
     */
    public GAg(int BHRSize, int SCSize) {
        // Initialize the BHR register with the given size and no default value
        this.BHR = new ShiftRegister(BHRSize);

        // Initialize the PHT with a size of 2^BHRSize and each entry having a saturating counter of size "SCSize"
        Bit[] defaultBlock = getDefaultBlock(SCSize);
        this.PHT = new Cache<>(BHRSize, defaultBlock);

        // Initialize the SC register
        this.SC = new ShiftRegister(SCSize);
    }

    /**
     * Predicts the result of a branch instruction based on the global branch history.
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        Bit[] history = BHR.getValues();
        Bit[] counter = PHT.get(history);
        return counter[Bit.toNumber(counter)] == Bit.ZERO ? BranchResult.NOT_TAKEN : BranchResult.TAKEN;
    }

    /**
     * Updates the values in the cache based on the actual branch result.
     *
     * @param instruction the branch instruction
     * @param actual      the actual result of the branch condition
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        Bit[] history = BHR.getValues();
    Bit[] counter = PHT.get(history);
    int index = Bit.toNumber(counter);
    if (actual == BranchResult.TAKEN) {
        if (counter[index] != Bit.ONE) {
            counter[index] = Bit.values()[counter[index].ordinal() + 1];
        }
    } else {
        if (counter[index] != Bit.ZERO) {
            counter[index] = Bit.values()[counter[index].ordinal() - 1];
        }
    }
    PHT.update(history, counter);
    BHR.shiftAndSet(actual == BranchResult.TAKEN ? Bit.ONE : Bit.ZERO);
    }

    /**
     * @return a zero series of bits as the default value of the cache block
     */
    private Bit[] getDefaultBlock(int size) {
        Bit[] defaultBlock = new Bit[size];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "GAg predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
