package com.pg85.otg.forge.materials;

import com.pg85.otg.common.LocalMaterialData;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.exception.InvalidConfigException;
import com.pg85.otg.forge.OTGPlugin;
import com.pg85.otg.forge.blocks.BlockPortalOTG;
import com.pg85.otg.forge.blocks.ModBlocks;
import com.pg85.otg.util.helpers.BlockHelper;
import com.pg85.otg.util.materials.MaterialHelper;
import com.pg85.otg.util.minecraft.defaults.DefaultMaterial;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

//TODO: Clean up and optimise ForgeMaterialData/BukkitMaterialData/LocalMaterialData/MaterialHelper/OTGEngine.readMaterial
/**
 * Implementation of LocalMaterial that wraps one of Minecraft's Blocks.
 *
 */
public class ForgeMaterialData implements LocalMaterialData
{
	DefaultMaterial defaultMaterial;
    private IBlockState blockData;
    private boolean checkFallbacks = false;
    private String rawEntry;
    private boolean isBlank = false;
    boolean metaIdSet = false;
    byte metaId;
    boolean materialIdSet = false;
    int materialId;
    private String name;
    
    private ForgeMaterialData(IBlockState blockData, int blockId, int blockMetaData)
    {
        this.blockData = blockData;
        this.materialIdSet = true;
        this.materialId = blockId;
        this.metaIdSet = true;
        this.metaId = (byte)blockMetaData;
    }
    
    private ForgeMaterialData(IBlockState blockData)
    {
        this.blockData = blockData;
    }
    
    private ForgeMaterialData(String raw)
    {
    	this.blockData = null;
    	this.rawEntry = raw;
    	this.checkFallbacks = true;
    }
    
    public static ForgeMaterialData getBlank()
    {
    	ForgeMaterialData material = new ForgeMaterialData((IBlockState)null);
    	material.isBlank = true;
    	material.checkFallbacks = false;
    	return material;
    }

    public static ForgeMaterialData ofString(String input) throws InvalidConfigException
    {
        // Try parsing as an internal Minecraft name
        // This is so that things like "minecraft:stone" aren't parsed
        // as the block "minecraft" with data "stone", but instead as the
        // block "minecraft:stone" with no block data.

    	// Used in BO4's as placeholder/detector block.
    	if(input.toLowerCase().equals("blank"))
    	{
    		return ForgeMaterialData.getBlank();
    	}

    	String newInput = input;

        net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromName(newInput);
        if (block != null)
        {
        	// Some old apps exported schematics/bo3's exported "STAIRS" without metadata (for instance "STAIRS:0").
        	// However, the default rotation has changed so fix this by adding the correct metadata.

        	if(
    			block == Blocks.PORTAL ||
				block == Blocks.DISPENSER ||
    			block == Blocks.ACACIA_STAIRS ||
        		block == Blocks.BIRCH_STAIRS ||
        		block == Blocks.BRICK_STAIRS ||
        		block == Blocks.DARK_OAK_STAIRS ||
        		block == Blocks.JUNGLE_STAIRS ||
        		block == Blocks.NETHER_BRICK_STAIRS ||
        		block == Blocks.OAK_STAIRS ||
        		block == Blocks.PURPUR_STAIRS ||
        		block == Blocks.QUARTZ_STAIRS ||
        		block == Blocks.RED_SANDSTONE_STAIRS ||
        		block == Blocks.SANDSTONE_STAIRS ||
        		block == Blocks.SPRUCE_STAIRS ||
        		block == Blocks.STONE_BRICK_STAIRS ||
        		block == Blocks.STONE_STAIRS
    		)
        	{
        		newInput = input + ":0"; // TODO: Shouldn't this be 3? This appears to fix the problem for the dungeon dimension but I still see it in BB, double check?
        	} else {
	            return ForgeMaterialData.ofMinecraftBlock(block);
        	}
        }

        try
        {
            // Try block(:data) syntax
            return getMaterial0(newInput);
        } catch (NumberFormatException e)
        {
            throw new InvalidConfigException("Unknown material: " + input);
        }
    }

    private static ForgeMaterialData getMaterial0(String input) throws NumberFormatException, InvalidConfigException
    {
        String blockName = input;
        int blockData = -1;

        // When there is a . or a : in the name, extract block data
        int splitIndex = input.lastIndexOf(":");
        if (splitIndex == -1)
        {
            splitIndex = input.lastIndexOf(".");
        }
        if (splitIndex != -1)
        {
            blockName = input.substring(0, splitIndex);
            blockData = Integer.parseInt(input.substring(splitIndex + 1));
        }

        // Parse block name
        Block block = Block.getBlockFromName(blockName);
        if (block == null)
        {
            DefaultMaterial defaultMaterial = DefaultMaterial.getMaterial(blockName);
            if (defaultMaterial != DefaultMaterial.UNKNOWN_BLOCK)
            {
                block = Block.getBlockById(defaultMaterial.id);

            	// Some old apps exported schematics/bo3's exported "STAIRS" without metadata (for instance "STAIRS:0").
            	// However, the default rotation has changed so fix this by adding the correct metadata.

                // TODO: Check if the block uses the Facing property instead of checking a list of known blocks?
            	if(
        			blockData == -1 &&
        			(
    					block == Blocks.PORTAL ||
    					block == Blocks.DISPENSER ||
	        			block == Blocks.ACACIA_STAIRS ||
	            		block == Blocks.BIRCH_STAIRS ||
	            		block == Blocks.BRICK_STAIRS ||
	            		block == Blocks.DARK_OAK_STAIRS ||
	            		block == Blocks.JUNGLE_STAIRS ||
	            		block == Blocks.NETHER_BRICK_STAIRS ||
	            		block == Blocks.OAK_STAIRS ||
	            		block == Blocks.PURPUR_STAIRS ||
	            		block == Blocks.QUARTZ_STAIRS ||
	            		block == Blocks.RED_SANDSTONE_STAIRS ||
	            		block == Blocks.SANDSTONE_STAIRS ||
	            		block == Blocks.SPRUCE_STAIRS ||
	            		block == Blocks.STONE_BRICK_STAIRS ||
	            		block == Blocks.STONE_STAIRS
            		)
        		)
            	{
            		blockData = 0; // TODO: Shouldn't this be 3? This appears to fix the problem for the dungeon dimension but I still see it in BB, double check?
            	}
            }
        }

        // Get the block
        if (block != null)
        {
            if (blockData == -1)
            {
                // Use default
                return ForgeMaterialData.ofMinecraftBlock(block);
            } else {
                // Use specified data
                try
                {
                    return ForgeMaterialData.ofMinecraftBlockState(block.getStateFromMeta(blockData));
                }
                catch(java.lang.ArrayIndexOutOfBoundsException e)
                {
                	throw new InvalidConfigException("Illegal meta data for the block type, cannot use " + input);
                }
                catch (IllegalArgumentException e)
                {
                	throw new InvalidConfigException("Illegal block data for the block type, cannot use " + input);
                }
            }
        }

        // Failed, try parsing later as a fallback.
        return new ForgeMaterialData(input);
    }
    
    /**
     * Gets a {@code ForgeMaterialData} of the given id and data.
     * @param id   The block id.
     * @param data The block data.
     * @return The {@code BukkitMateialData} instance.
     */
    @SuppressWarnings("deprecation")
    private static ForgeMaterialData ofIds(int id, int data)
    {
        Block block = Block.getBlockById(id);
        IBlockState blockData = block.getStateFromMeta(data);
        return new ForgeMaterialData(blockData, id, data);
    }

    /**
     * Gets a {@code BukkitMaterialData} of the given material and data.
     * @param material The material.
     * @param data     The block data.
     * @return The {@code BukkitMateialData} instance.
     */
    public static ForgeMaterialData ofDefaultMaterial(DefaultMaterial material, int data)
    {
        return ofIds(material.id, data);
    }

    /**
     * Gets a {@code BukkitMaterialData} of the given Minecraft block. The
     * default block data (usually 0) will be used.
     * @param block The material.
     * @return The {@code BukkitMateialData} instance.
     */
    public static ForgeMaterialData ofMinecraftBlock(Block block)
    {
        return ofMinecraftBlockState(block.getDefaultState());
    }

    /**
     * Gets a {@code ForgeMaterialData} of the given Minecraft blockData.
     * @param blockData The material an data.
     * @return The {@code BukkitMateialData} instance.
     */
    public static ForgeMaterialData ofMinecraftBlockState(IBlockState blockData)
    {
        return new ForgeMaterialData(blockData);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public LocalMaterialData withBlockData(int i)
    {
    	if(this.blockData == null)
    	{
    		return this;
    	}
        if (i == getBlockData())
        {
            return this;
        }

        Block block = this.blockData.getBlock();
        return ofMinecraftBlockState(block.getStateFromMeta(i));
    }

    @Override
    public LocalMaterialData withDefaultBlockData()
    {
    	if(this.blockData == null)
    	{
    		return this;
    	}
        Block block = this.blockData.getBlock();
        return this.withBlockData(block.getMetaFromState(block.getDefaultState()));
    }
    
    @Override
    public byte getBlockData()
    {
    	if(!this.metaIdSet)
    	{
    		this.metaIdSet = true;
    		this.metaId = this.blockData == null ? 0 : (byte) this.blockData.getBlock().getMetaFromState(this.blockData);
    	}
        return this.metaId;
    }

    @Override
    public int getBlockId()
    {
    	if(!this.materialIdSet)
    	{
    		this.materialIdSet = true;
    		this.materialId = this.blockData == null ? 0 : Block.getIdFromBlock(this.blockData.getBlock());
    	}
        return this.materialId;
    }

    @Override
    public String getName()
    {
    	if(this.name == null)
    	{
	    	if(isBlank)
	    	{
	    		this.name = "BLANK";
	    	}
	    	else if(this.blockData == null)
	    	{
	    		this.name = "Unknown";
	    	} else {
		        Block block = this.blockData.getBlock();
		        DefaultMaterial defaultMaterial = toDefaultMaterial();
		
		        byte data = getBlockData();
		        boolean nonDefaultData = !block.getDefaultState().equals(this.blockData);
		        boolean noData = this.blockData.getPropertyKeys().isEmpty();
		        // Note that the above line is not equivalent to data != 0, as for
		        // example pumpkins have a default data value of 2
		
		        if (defaultMaterial == DefaultMaterial.UNKNOWN_BLOCK)
		        {
		            // Use Minecraft's name
		            if (nonDefaultData)
		            {
		            	this.name = Block.REGISTRY.getNameForObject(block) + (noData ? "" : ":" + data);
		            } else {
		            	this.name = Block.REGISTRY.getNameForObject(block).toString();
		            }
		        } else {
		            // Use our name
		        	this.name = defaultMaterial.name() + (noData ? "" : ":" + data);
		        }
	    	}
    	}
    	return this.name;
    }

    public IBlockState internalBlock()
    {
        return this.blockData;
    }

    @Override
    public boolean isMaterial(DefaultMaterial material)
    {
        return material.id == getBlockId();
    }
    
    @Override
    public boolean isLiquid()
    {
    	// For some reason, .isLiquid() appears to be 
    	// really slow, so use defaultMaterial instead.
    	
        // Let us override whether materials are solid
        DefaultMaterial defaultMaterial = toDefaultMaterial();
        if (defaultMaterial != DefaultMaterial.UNKNOWN_BLOCK)
        {
            return defaultMaterial.isLiquid();
        }
        
        return this.blockData == null ? false : this.blockData.getMaterial().isLiquid();
    }

    @Override
    public boolean isSolid()
    {   	
        // Let us override whether materials are solid
        DefaultMaterial defaultMaterial = toDefaultMaterial();
        if (defaultMaterial != DefaultMaterial.UNKNOWN_BLOCK)
        {
            return defaultMaterial.isSolid();
        }

        return this.blockData == null ? false : this.blockData.getMaterial().isSolid();
    }
    
    @Override
    public boolean isEmptyOrAir()
    {
        return this.blockData == null ? true : this.blockData.getBlock() == Blocks.AIR;
    }
    
    @Override
    public boolean isAir()
    {
        return this.blockData != null && this.blockData.getBlock() == Blocks.AIR;
    }
    
    @Override
    public boolean isEmpty()
    {
        return this.blockData == null;
    }

    @Override
    public boolean canFall()
    {
        return this.blockData == null ? false : this.blockData.getBlock() instanceof BlockFalling;
    }

    @Override
    public boolean canSnowFallOn()
    {
        DefaultMaterial defaultMaterial = toDefaultMaterial();
        if (defaultMaterial != DefaultMaterial.UNKNOWN_BLOCK)
        {
            return defaultMaterial.canSnowFallOn();
        }

        return this.blockData == null ? false : this.blockData.getMaterial().isSolid();    	
    }
    
    @Override
    public boolean isSmoothAreaAnchor(boolean allowWood, boolean ignoreWater)
    {
    	DefaultMaterial defaultMaterial = toDefaultMaterial();
    	return
    		(
				defaultMaterial.equals(DefaultMaterial.ICE) ||
				defaultMaterial.equals(DefaultMaterial.PACKED_ICE) ||
				defaultMaterial.equals(DefaultMaterial.FROSTED_ICE) ||
				(
					isSolid() || 
					(
						!ignoreWater && isLiquid()
					)
				)
			) &&
			(
				allowWood || 
				!(
					defaultMaterial.equals(DefaultMaterial.LOG) || 
					defaultMaterial.equals(DefaultMaterial.LOG_2)
				)
			) &&
			!defaultMaterial.equals(DefaultMaterial.WATER_LILY);
    }
    
    @Override
    public LocalMaterialData rotate()
    {
        // Try to rotate
        DefaultMaterial defaultMaterial = toDefaultMaterial();
        if (defaultMaterial != DefaultMaterial.UNKNOWN_BLOCK)
        {
            // We only know how to rotate vanilla blocks
            byte blockDataByte = getBlockData();
            int newData = BlockHelper.rotateData(defaultMaterial, blockDataByte);
            if (newData != blockDataByte)
            {
            	// Don't return a copy, return a cached object. OTG should only use forgematerialdata for BO2's/BO3's/BO4's and materialset,
            	// and shouldn't need to edit them, so they can be re-used. TODO: Make sure this won't cause problems.
            	try
            	{
					return MaterialHelper.readMaterial(defaultMaterial.name() + ":" + newData);
				}
            	catch (InvalidConfigException e)
            	{
					e.printStackTrace();
					return null;
				}
                //return ofMinecraftBlockState(this.blockData.getBlock().getStateFromMeta(newData));
            }
        }

        // No changes, return object itself
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public LocalMaterialData rotate(int rotateTimes)
    {
        // Try to rotate
        DefaultMaterial defaultMaterial = toDefaultMaterial();
        if (defaultMaterial != DefaultMaterial.UNKNOWN_BLOCK)
        {
            // We only know how to rotate vanilla blocks
        	byte blockDataByte = 0;
            int newData = 0;
            for(int i = 0; i < rotateTimes; i++)
            {
            	blockDataByte = getBlockData();
            	newData = BlockHelper.rotateData(defaultMaterial, blockDataByte);	
            }
            if (newData != blockDataByte)
            {
                return ofMinecraftBlockState(this.blockData.getBlock().getStateFromMeta(newData));
            }
        }

        // No changes, return object itself
        return this;
    }    

	@Override
	public LocalMaterialData parseForWorld(LocalWorld world)
	{
		if (this.checkFallbacks)
		{
			this.checkFallbacks = false;
			ForgeMaterialData newMaterialData = ((ForgeMaterialData)world.getConfigs().getWorldConfig().parseFallback(this.rawEntry)); 
			if(newMaterialData != null && newMaterialData.blockData != null && !newMaterialData.blockData.equals(this.blockData))
			{
				this.blockData = newMaterialData.blockData;
				this.metaIdSet = false;
				this.materialIdSet = false;
				this.rawEntry = null;
				this.defaultMaterial = null;
				this.name = null;
			}
		}
		return this;
	}

    @Override
    public DefaultMaterial toDefaultMaterial()
    {
    	if(this.defaultMaterial == null)
    	{
        	if(this.blockData == null)
        	{
        		this.defaultMaterial = DefaultMaterial.UNKNOWN_BLOCK;
        	} else {
        		this.defaultMaterial = DefaultMaterial.getMaterial(getBlockId());
        		if(this.defaultMaterial == DefaultMaterial.UNKNOWN_BLOCK)
        		{
            		if(this.blockData.getBlock() instanceof BlockPortalOTG) // TODO: avoid using instanceof so much?
            		{
        				this.defaultMaterial = DefaultMaterial.PORTAL;
            		}
        		}
        	}
    	}
    	return defaultMaterial;
    }

	@Override
	public boolean isParsed()
	{
		return !checkFallbacks;
	}
    
    @Override
    public int hashCode()
    {
        // From 4096 to 69632 when there are 4096 block ids
        return PluginStandardValues.SUPPORTED_BLOCK_IDS + (getBlockId() * 16) + getBlockData();
    }

    @Override
    public int hashCodeWithoutBlockData()
    {
        // From 0 to 4095 when there are 4096 block ids
        return getBlockId();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof ForgeMaterialData))
        {
            return false;
        }
        ForgeMaterialData other = (ForgeMaterialData) obj;
        return this.blockData.equals(other.blockData);
    }
    
    @Override
    public String toString()
    {
        return getName();
    }
}
