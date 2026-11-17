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
package net.shadowmage.ancientwarfare.core.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/*
 * Port notes: the Forge 1.12 Configuration (.cfg) is replaced by a ModConfigSpec (COMMON, "ancientwarfare-<module>.toml").
 * Subclasses keep their static holder fields:
 *   initializeCategories() + initializeValues() now run against the spec builder ("config") while the spec is built,
 *   loadValues() copies the spec values into the static fields on ModConfigEvent (1.12 defaults stay until then).
 */
public abstract class ModConfiguration {
	public static final String generalOptions = "01_shared_settings";
	public static final String serverOptions = "02_server_settings";
	public static final String clientOptions = "03_client_settings";
	public static final String configPathForFiles = "config/ancientwarfare/";

	protected ModConfigSpec.Builder config;
	private final ModConfigSpec spec;
	private final String fileName;
	public boolean updatedVersion = false;
	public boolean autoExportOnUpdate = false;

	public ModConfiguration(String modid) {
		this.fileName = modid + ".toml";
		config = new ModConfigSpec.Builder();
		initializeCategories();
		initializeValues();
		spec = config.build();
		config = null;
	}

	/*
	 * Must be called from the module init (mod construction).
	 */
	public void register(IEventBus modBus, ModContainer container) {
		container.registerConfig(ModConfig.Type.COMMON, spec, fileName);
		modBus.addListener((ModConfigEvent.Loading evt) -> onConfigEvent(evt));
		modBus.addListener((ModConfigEvent.Reloading evt) -> onConfigEvent(evt));
	}

	private void onConfigEvent(ModConfigEvent evt) {
		if (evt.getConfig().getSpec() == spec) {
			loadValues();
		}
	}

	protected abstract void initializeCategories();

	protected abstract void initializeValues();

	protected abstract void loadValues();

	public ModConfigSpec getConfig() {
		return this.spec;
	}

	/*
	 * 1.12 saved the .cfg when it had changed; ModConfigSpec values save themselves when set.
	 */
	public void save() {
	}

	public boolean updatedVersion() {
		return updatedVersion;
	}

	public boolean autoExportOnUpdate() {
		return autoExportOnUpdate;
	}
}
