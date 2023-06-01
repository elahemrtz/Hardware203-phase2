package hardwar.branch.prediction.judged.GAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

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

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        int phtSize = (int) Math.pow(2, BHRSize);
        this.PHT = new Cache<>(phtSize, getDefaultBlock());

        // Initialize the SC register
        this.SC = new ShiftRegister(SCSize);
    }

    /**
     * Predicts the result of a branch instruction based on the global branch history
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO : complete Task 1
        Bit[] history = BHR.read();
        Bit[] phtEntry = PHT.get(history);
        Bit counter = phtEntry[0];

        if (actual == BranchResult.TAKEN) {
            if (counter == Bit.ONE || counter == Bit.TWO)
                phtEntry[0] = Bit.THREE;
            else if (counter == Bit.ZERO)
                phtEntry[0] = Bit.ONE;
        } else {
            if (counter == Bit.ONE || counter == Bit.TWO)
                phtEntry[0] = Bit.ZERO;
            else if (counter == Bit.THREE)
                phtEntry[0] = Bit.TWO;
        }

        PHT.put(history, phtEntry);
        BHR.insert(actual == BranchResult.TAKEN ? Bit.ONE : Bit.ZERO);
        SC.insert(actual == BranchResult.TAKEN ? Bit.ONE : Bit.ZERO);
    }

    /**
     * Updates the values in the cache based on the actual branch result
     *
     * @param instruction the branch instruction
     * @param actual      the actual result of the branch condition
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO: complete Task 2
    }


    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "GAg predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
