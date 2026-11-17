/*
 Copyright 2012-2013 John Cummens (aka Shadowmage, Shadowmage4513)
 This software is distributed under the terms of the GNU General Public License.
 Please see COPYING for precise license information.

 This file is part of Ancient Warfare.

 Ancient Warfare is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Ancient Warfare is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Ancient Warfare.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.shadowmage.ancientwarfare.automation.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.shadowmage.ancientwarfare.core.config.ModConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Port notes: file is config/ancientwarfare-automation.toml, same keys and defaults as the 1.12 .cfg.
 * - rf_energy_conversion_factor is kept (and still fills torqueToRf / rfToTorque) but nothing uses it: the optional
 *   RedstoneFlux integration is dropped.
 * - animal_farm_pickups: the 1.12 default "minecraft:wool" (any colour) is the 16 flattened wool items.
 * - render_work_bounds was a 1.12 string Property ("true"/"false") toggled by a key; it is a boolean here
 *   (renderWorkBounds / setRenderWorkBounds). Both client options live in the same file (category 03_client_settings).
 */
public class AWAutomationStatics extends ModConfiguration {

	public static int conduitRenderID = -1;
	public static int fishFarmRescanTicks = 200;
	public static int animalFarmRescanTicks = 200;

	/*
	 * Travel time per block when sending/receiving items using the mailbox system<br>
	 * Distances are calculated as a floating point distance and rounded to the nearest whole<br>
	 */
	public static int mailboxTimePerBlock = 10;

	/*
	 * Travel time for mail using mailboxes when items are being sent/received in different dimensions
	 */
	public static int mailboxTimeForDimension = 100;

	/*
	 * used to reduce network updates
	 */
	public static int energyMinNetworkUpdateFrequency = 5;//default 4 updates/sec max; less if not needed
	public static boolean enable_energy_network_updates = true;
	public static boolean enable_energy_client_updates = true;

	public static double low_efficiency_factor = 0.99990d;
	public static double med_efficiency_factor = 0.99995d;
	public static double high_efficiency_factor = 0.99999d;

	public static double low_transfer_max = 32.d;
	public static double med_transfer_max = 192.d;
	public static double high_transfer_max = 1024.d;

	public static double low_conduit_energy_max = 32.d;
	public static double med_conduit_energy_max = 192.d;
	public static double high_conduit_energy_max = 1024.f;

	public static double low_storage_energy_max = 9600.d;
	public static double med_storage_energy_max = 14400.d;
	public static double high_storage_energy_max = 24000.d;

	public static double stirling_generator_output = 1.d;
	public static double waterwheel_generator_output = 1.d;
	public static double hand_cranked_generator_output = 1.d;
	public static double windmill_per_size_output = 1.d;

	public static final float rpmToRpt = (float) (360.d / 60.d / 20.d);
	public static final float low_quality_rpm = 100;
	public static final float low_rpt = low_quality_rpm * rpmToRpt;

	public static double rfToTorque = 1.d / 10.d;
	public static final double mjToTorque = 1.d;
	public static double torqueToRf = 10.d;
	public static final double torqueToMj = 1.d;

	public static boolean renderWorkBounds = true;

	public static String[] animal_farm_pickups = new String[] {"minecraft:leather",
			"minecraft:beef",
			"minecraft:cooked_beef",
			"minecraft:chicken",
			"minecraft:cooked_chicken",
			"minecraft:feather",
			"minecraft:egg",
			"minecraft:white_wool",
			"minecraft:orange_wool",
			"minecraft:magenta_wool",
			"minecraft:light_blue_wool",
			"minecraft:yellow_wool",
			"minecraft:lime_wool",
			"minecraft:pink_wool",
			"minecraft:gray_wool",
			"minecraft:light_gray_wool",
			"minecraft:cyan_wool",
			"minecraft:purple_wool",
			"minecraft:blue_wool",
			"minecraft:brown_wool",
			"minecraft:green_wool",
			"minecraft:red_wool",
			"minecraft:black_wool",
			"minecraft:cooked_porkchop",
			"minecraft:porkchop",
			"harderwildlife:rawleather"};

	private static ModConfigSpec.BooleanValue renderWorkBoundsValue;

	private ModConfigSpec.DoubleValue torqueToRfValue;
	private ModConfigSpec.IntValue energyMinNetworkUpdateFrequencyValue;
	private ModConfigSpec.BooleanValue enableEnergyNetworkUpdatesValue;
	private ModConfigSpec.IntValue mailboxTimePerBlockValue;
	private ModConfigSpec.IntValue mailboxTimeForDimensionValue;
	private ModConfigSpec.DoubleValue lowEfficiencyFactorValue;
	private ModConfigSpec.DoubleValue medEfficiencyFactorValue;
	private ModConfigSpec.DoubleValue highEfficiencyFactorValue;
	private ModConfigSpec.DoubleValue lowTransferMaxValue;
	private ModConfigSpec.DoubleValue medTransferMaxValue;
	private ModConfigSpec.DoubleValue highTransferMaxValue;
	private ModConfigSpec.DoubleValue lowConduitEnergyMaxValue;
	private ModConfigSpec.DoubleValue medConduitEnergyMaxValue;
	private ModConfigSpec.DoubleValue highConduitEnergyMaxValue;
	private ModConfigSpec.DoubleValue lowStorageEnergyMaxValue;
	private ModConfigSpec.DoubleValue medStorageEnergyMaxValue;
	private ModConfigSpec.DoubleValue highStorageEnergyMaxValue;
	private ModConfigSpec.DoubleValue stirlingGeneratorOutputValue;
	private ModConfigSpec.DoubleValue waterwheelGeneratorOutputValue;
	private ModConfigSpec.DoubleValue handCrankedGeneratorOutputValue;
	private ModConfigSpec.DoubleValue windmillPerSizeOutputValue;
	private ModConfigSpec.BooleanValue enableEnergyClientUpdatesValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> animalFarmPickupsValue;

	public AWAutomationStatics(String mod) {
		super(mod);
	}

	@Override
	public void initializeCategories() {
		config.comment("General Options\n" + "Affect both client and server.\nThese configs must match for client and server, or strange and probably BAD things WILL happen.").push(generalOptions).pop();
		config.comment("Server Options\n" + "Affect only server-side operations.\nWill need to be set for dedicated servers, and single player (or LAN worlds).\nClients playing on remote servers can ignore these settings.").push(serverOptions).pop();
		config.comment("Client Options\n" + "Affect only client-side operations.\nMany of these options can be set from the in-game Options GUI.\n" + "Server admins can ignore these settings.").push(clientOptions).pop();
	}

	private ModConfigSpec.DoubleValue dbl(String name, double def, String comment) {
		return config.comment(comment).defineInRange(name, def, -Double.MAX_VALUE, Double.MAX_VALUE);
	}

	private ModConfigSpec.IntValue integer(String name, int def, String comment) {
		return config.comment(comment).defineInRange(name, def, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public void initializeValues() {
		config.push(generalOptions);
		torqueToRfValue = dbl("rf_energy_conversion_factor", torqueToRf, "How much rf energy units for one unit of torque based energy.\n" + "Default= " + torqueToRf + "\n" + "Zero or negative values stop rf energy from being used in automated blocks.");
		energyMinNetworkUpdateFrequencyValue = integer("energy_network_update_frequency", energyMinNetworkUpdateFrequency, "Alter the frequency at which network updates are sent to clients.\n" + "Default= " + energyMinNetworkUpdateFrequency + "\n" + "Lower values send data more often.\nHigher values send less often.\nZero or negative values send every tick.");
		config.pop();

		config.push(serverOptions);
		enableEnergyNetworkUpdatesValue = config.comment("Enable/Disable Sending network updates for energy tiles.\n" + "Default = " + enable_energy_network_updates + "\n" + "Disabling may improve server network performance on congested/low-bandwith deployments.").define("enable_server_energy_network", enable_energy_network_updates);
		mailboxTimePerBlockValue = integer("mailbox_travel_time_per_block", mailboxTimePerBlock, "Ticks per block to be traveled for teleporting items.\n" + "Default= " + mailboxTimePerBlock + "\n" + "Higher values increase travel time for items.\nLower values reduce travel time.\n" + "Zero or negative values result in instant transfer.");
		mailboxTimeForDimensionValue = integer("mailbox_travel_time_per_dimension", mailboxTimeForDimension, "Ticks for dimensional travel for teleporting items.\n" + "Default= " + mailboxTimeForDimension + "\n" + "Higher values increase travel time for items.\nLower values reduce travel time.\n" + "Zero or negative values result in instant transfer.");
		lowEfficiencyFactorValue = dbl("low_quality_tile_energy_drain", low_efficiency_factor, "Factor applied to base drain algorithm to determine energy loss for low-quality torque tiles.\n" + "Default = " + low_efficiency_factor + "\n" + "Higher values result in more energy drain.\nLower values result in less.\n" + "Negative values will result in a feedback loop of free/infinite power.");
		medEfficiencyFactorValue = dbl("med_quality_tile_energy_drain", med_efficiency_factor, "Factor applied to base drain algorithm to determine energy loss for medium-quality torque tiles.\n" + "Default = " + med_efficiency_factor + "\n" + "Higher values result in more energy drain.\nLower values result in less.\n" + "Negative values will result in a feedback loop of free/infinite power.");
		highEfficiencyFactorValue = dbl("high_quality_tile_energy_drain", high_efficiency_factor, "Factor applied to base drain algorithm to determine energy loss for high-quality torque tiles.\n" + "Default = " + high_efficiency_factor + "\n" + "Higher values result in more energy drain.\nLower values result in less.\n" + "Negative values will result in a feedback loop of free/infinite power.");
		lowTransferMaxValue = dbl("low_quality_tile_energy_transfer", low_transfer_max, "How much energy may be output per tick by low-quality torque tiles.\n" + "Default = " + low_transfer_max + "\n" + "Higher values result in more thoroughput of energy network.\nLower values result in less.\n" + "Negative values will cause energy transfer to cease functioning.");
		medTransferMaxValue = dbl("med_quality_tile_energy_transfer", med_transfer_max, "How much energy may be output per tick by medium-quality torque tiles.\n" + "Default = " + med_transfer_max + "\n" + "Higher values result in more thoroughput of energy network.\nLower values result in less.\n" + "Negative values will cause energy transfer to cease functioning.");
		highTransferMaxValue = dbl("high_quality_tile_energy_transfer", high_transfer_max, "How much energy may be output per tick by high-quality torque tiles.\n" + "Default = " + high_transfer_max + "\n" + "Higher values result in more thoroughput of energy network.\nLower values result in less.\n" + "Negative values will cause energy transfer to cease functioning.");
		lowConduitEnergyMaxValue = dbl("low_quality_conduit_energy_max", low_conduit_energy_max, "How much energy may be stored in low-quality energy transport tiles.\n" + "Default = " + low_conduit_energy_max + "\n" + "Directly sets the amount of torque/MJ that a transport conduit may store internally.");
		medConduitEnergyMaxValue = dbl("med_quality_conduit_energy_max", med_conduit_energy_max, "How much energy may be stored in medium-quality energy transport tiles.\n" + "Default = " + med_conduit_energy_max + "\n" + "Directly sets the amount of torque/MJ that a transport conduit may store internally.");
		highConduitEnergyMaxValue = dbl("high_quality_conduit_energy_max", high_conduit_energy_max, "How much energy may be stored in high-quality energy transport tiles.\n" + "Default = " + high_conduit_energy_max + "\n" + "Directly sets the amount of torque/MJ that a transport conduit may store internally.");
		lowStorageEnergyMaxValue = dbl("low_quality_storage_energy_max", low_storage_energy_max, "How much energy may be stored in low-quality energy storage tiles.\n" + "Default = " + low_storage_energy_max + "\n" + "Directly sets the amount of torque/MJ that a storage tile may store internally.");
		medStorageEnergyMaxValue = dbl("med_quality_storage_energy_max", med_storage_energy_max, "How much energy may be stored in medium-quality energy storage tiles.\n" + "Default = " + med_storage_energy_max + "\n" + "Directly sets the amount of torque/MJ that a storage tile may store internally.");
		highStorageEnergyMaxValue = dbl("high_quality_storage_energy_max", high_storage_energy_max, "How much energy may be stored in high-quality energy storage tiles.\n" + "Default = " + high_storage_energy_max + "\n" + "Directly sets the amount of torque/MJ that a storage tile may store internally.");
		stirlingGeneratorOutputValue = dbl("stirling_generator_output_factor", stirling_generator_output, "Factor applied to energy output from stirling generator.\n" + "Default = " + stirling_generator_output + "\n" + "Lower values reduce output.\nHigher values increase output.\n" + "Zero or negative values will result in no energy output");
		waterwheelGeneratorOutputValue = dbl("waterwheel_generator_output_factor", waterwheel_generator_output, "Factor applied to energy output from waterwheel generator.\n" + "Default = " + waterwheel_generator_output + "\n" + "Lower values reduce output.\nHigher values increase output.\n" + "Zero or negative values will result in no energy output");
		handCrankedGeneratorOutputValue = dbl("hand_cranked_generator_output_factor", hand_cranked_generator_output, "Factor applied to energy output from hand-cranked generator.\n" + "Default = " + hand_cranked_generator_output + "\n" + "Lower values reduce output.\nHigher values increase output.\n" + "Zero or negative values will result in no energy output");
		windmillPerSizeOutputValue = dbl("windmill_per_size_output_factor", windmill_per_size_output, "Factor applied to energy output from windmill based on size.\n" + "Default = " + windmill_per_size_output + "\n" + "Lower values reduce output.\nHigher values increase output.\n" + "Zero or negative values will result in no energy output");
		animalFarmPickupsValue = config.comment("List of items that Animal Farms can pickup\n" + "Animal farms will periodically pickup any of these items detected (if they're not caught on-demand by the farmer culling animals).\n" + "Use the format modid:itemid").defineListAllowEmpty("animal_farm_pickups", new ArrayList<>(Arrays.asList(animal_farm_pickups)), () -> "", o -> o instanceof String);
		config.pop();

		config.push(clientOptions);
		enableEnergyClientUpdatesValue = config.comment("Enable client-side animation of power tiles.\nDisabling may improve rendering performance on low-end machines").define("enable_client_energy_animations", enable_energy_client_updates);
		renderWorkBoundsValue = config.define("render_work_bounds", true);
		config.pop();
	}

	@Override
	protected void loadValues() {
		double temp = torqueToRfValue.get();
		if (temp <= 0.0D) {
			torqueToRf = 0;
			rfToTorque = 0;
		} else {
			torqueToRf = temp;
			rfToTorque = 1 / temp;
		}
		energyMinNetworkUpdateFrequency = energyMinNetworkUpdateFrequencyValue.get();
		enable_energy_network_updates = enableEnergyNetworkUpdatesValue.get();
		mailboxTimePerBlock = mailboxTimePerBlockValue.get();
		mailboxTimeForDimension = mailboxTimeForDimensionValue.get();
		low_efficiency_factor = lowEfficiencyFactorValue.get();
		med_efficiency_factor = medEfficiencyFactorValue.get();
		high_efficiency_factor = highEfficiencyFactorValue.get();
		low_transfer_max = lowTransferMaxValue.get();
		med_transfer_max = medTransferMaxValue.get();
		high_transfer_max = highTransferMaxValue.get();
		low_conduit_energy_max = lowConduitEnergyMaxValue.get();
		med_conduit_energy_max = medConduitEnergyMaxValue.get();
		high_conduit_energy_max = highConduitEnergyMaxValue.get();
		low_storage_energy_max = lowStorageEnergyMaxValue.get();
		med_storage_energy_max = medStorageEnergyMaxValue.get();
		high_storage_energy_max = highStorageEnergyMaxValue.get();
		stirling_generator_output = stirlingGeneratorOutputValue.get();
		waterwheel_generator_output = waterwheelGeneratorOutputValue.get();
		hand_cranked_generator_output = handCrankedGeneratorOutputValue.get();
		windmill_per_size_output = windmillPerSizeOutputValue.get();
		enable_energy_client_updates = enableEnergyClientUpdatesValue.get();
		renderWorkBounds = renderWorkBoundsValue.get();
		animal_farm_pickups = animalFarmPickupsValue.get().toArray(new String[0]);
	}

	/*
	 * 1.12: AutomationInputHandler toggled the "render_work_bounds" property and saved the config
	 */
	public static void setRenderWorkBounds(boolean render) {
		renderWorkBounds = render;
		if (renderWorkBoundsValue != null) {
			renderWorkBoundsValue.set(render);
		}
	}
}
