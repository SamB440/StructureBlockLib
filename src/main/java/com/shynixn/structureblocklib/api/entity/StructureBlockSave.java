package com.shynixn.structureblocklib.api.entity;

public interface StructureBlockSave extends StructureBlockConstruction {
    /**
     * Sets if invisibleBlocks should be visible
     *
     * @param showInvisibleBlocks showInvisibleBlocks
     */
    void showInvisibleBlocks(boolean showInvisibleBlocks);

    /**
     * Returns if invisbileBlocks are visible
     *
     * @return visible
     */
    boolean isShowingInvisibleBlocks();

    /**
     * Saves the structure into the worldSave
     */
    void save();
}
