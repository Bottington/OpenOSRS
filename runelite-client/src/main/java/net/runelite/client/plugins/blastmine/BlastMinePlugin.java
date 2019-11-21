/*
 * Copyright (c) 2018, Unmoon <https://github.com/Unmoon>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.blastmine;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Blast Mine",
	description = "Show helpful information for the Blast Mine minigame",
	tags = {"explode", "explosive", "mining", "minigame", "skilling"}
)
@Singleton
public class BlastMinePlugin extends Plugin
{
	@Getter(AccessLevel.PACKAGE)
	private final Map<WorldPoint, BlastMineRock> rocks = new HashMap<>();

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private BlastMineRockOverlay blastMineRockOverlay;

	@Inject
	private BlastMineOreCountOverlay blastMineOreCountOverlay;

	@Inject
	private BlastMinePluginConfig config;

	@Provides
	BlastMinePluginConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BlastMinePluginConfig.class);
	}

	@Getter(AccessLevel.PACKAGE)
	private boolean showOreOverlay;
	@Getter(AccessLevel.PACKAGE)
	private boolean showRockIconOverlay;
	@Getter(AccessLevel.PACKAGE)
	private boolean showTimerOverlay;
	@Getter(AccessLevel.PACKAGE)
	private boolean showWarningOverlay;
	@Getter(AccessLevel.PACKAGE)
	private Color timerColor;
	@Getter(AccessLevel.PACKAGE)
	private Color warningColor;

	@Override
	protected void startUp() throws Exception
	{
		updateConfig();

		overlayManager.add(blastMineRockOverlay);
		overlayManager.add(blastMineOreCountOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(blastMineRockOverlay);
		overlayManager.remove(blastMineOreCountOverlay);
		final Widget blastMineWidget = client.getWidget(WidgetInfo.BLAST_MINE);

		if (blastMineWidget != null)
		{
			blastMineWidget.setHidden(false);
		}
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();
		BlastMineRockType blastMineRockType = BlastMineRockType.getRockType(gameObject.getId());
		if (blastMineRockType == null)
		{
			return;
		}

		final BlastMineRock newRock = new BlastMineRock(gameObject, blastMineRockType);
		final BlastMineRock oldRock = rocks.get(gameObject.getWorldLocation());

		if (oldRock == null || oldRock.getType() != newRock.getType())
		{
			rocks.put(gameObject.getWorldLocation(), newRock);
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			rocks.clear();
		}
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		if (rocks.isEmpty())
		{
			return;
		}

		rocks.values().removeIf(rock ->
			(rock.getRemainingTimeRelative() == 1 && rock.getType() != BlastMineRockType.NORMAL) ||
				(rock.getRemainingFuseTimeRelative() == 1 && rock.getType() == BlastMineRockType.LIT));
	}

	private void updateConfig()
	{
		this.showOreOverlay = config.showOreOverlay();
		this.showRockIconOverlay = config.showRockIconOverlay();
		this.showTimerOverlay = config.showTimerOverlay();
		this.showWarningOverlay = config.showWarningOverlay();
		this.timerColor = config.getTimerColor();
		this.warningColor = config.getWarningColor();
	}
}
