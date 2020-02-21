/*
 * Copyright (c) 2020, Wild Adventure
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 4. Redistribution of this software in source or binary forms shall be free
 *    of all charges or fees to the recipient of this software.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gmail.filoghost.warpvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.filoghost.warpvp.config.ConfigBlock;
import com.gmail.filoghost.warpvp.config.ConfigLocation;
import com.gmail.filoghost.warpvp.config.GateRegion;
import com.gmail.filoghost.warpvp.config.Settings;
import com.gmail.filoghost.warpvp.config.WorldNotFoundException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.selections.Selection;

import net.cubespace.yamler.YamlerConfigurationException;
import wild.api.command.CommandFramework.Permission;
import wild.api.command.SubCommandFramework;

@Permission(Perms.COMMAND_USE)
public class CommandHandler extends SubCommandFramework {

	public CommandHandler(JavaPlugin plugin, String label) {
		super(plugin, label);
	}

	@Override
	public void noArgs(CommandSender sender) {
		sender.sendMessage(ChatColor.DARK_AQUA + "Comandi War:");
		for (SubCommandDetails command : getAccessibleSubCommands(sender)) {
			sender.sendMessage(ChatColor.AQUA + "/" + this.label + " " + command.getName() + (command.getUsage() != null ? " " + command.getUsage() : ""));
		}
	}
	
	@SubCommand("start")
	@SubCommandPermission(Perms.COMMAND_START)
	public void start(CommandSender sender, String label, String[] args) {
		if (!WarPvP.getInstance().openGates()) {
			WarPvP.getInstance().closeGates();
			throw new ExecuteException("Uno o entrambi i cancelli non sono stati impostati.");
		}
		
		sender.sendMessage(ChatColor.GREEN + "Cancelli aperti.");
	}
	
	@SubCommand("stop")
	@SubCommandPermission(Perms.COMMAND_STOP)
	public void stop(CommandSender sender, String label, String[] args) {
		if (!WarPvP.getInstance().closeGates()) {
			throw new ExecuteException("Uno o entrambi i cancelli non sono stati impostati.");
		}
		
		sender.sendMessage(ChatColor.GREEN + "Cancelli chiusi.");
	}
	
	@SubCommand("teleport")
	@SubCommandPermission(Perms.COMMAND_TELEPORT)
	@SubCommandUsage("<1|2> <giocatore>")
	@SubCommandMinArgs(2)
	public void teleport(CommandSender sender, String label, String[] args) {
		ConfigLocation configLocation;
		if (args[0].equals("1")) {
			configLocation = Settings.teleport1;
		} else if (args[0].equals("2")) {
			configLocation = Settings.teleport2;
		} else {
			throw new ExecuteException("Specifica 1 o 2.");
		}
		
		Player targetPlayer = Bukkit.getPlayerExact(args[1]);
		CommandValidate.notNull(targetPlayer, "Giocatore " + args[1] + " non trovato.");
		
		try {
			targetPlayer.teleport(configLocation.getBukkitLocation());
			sender.sendMessage(ChatColor.GREEN + "Giocatore " + targetPlayer.getName() + " mandato al teletrasporto #" + args[0] + ".");
		} catch (WorldNotFoundException e) {
			throw new ExecuteException("Teletrasporto #" + args[0] + " non impostato.");
		}
	}
	
	@SubCommand("setTeleport")
	@SubCommandPermission(Perms.COMMAND_SETTELEPORT)
	@SubCommandUsage("<1|2>")
	@SubCommandMinArgs(1)
	public void setTeleport(CommandSender sender, String label, String[] args) {
		
		ConfigLocation configLocation = new ConfigLocation(CommandValidate.getPlayerSender(sender).getLocation());
		
		if (args[0].equals("1")) {
			Settings.teleport1 = configLocation;
		} else if (args[0].equals("2")) {
			Settings.teleport2 = configLocation;
		} else {
			throw new ExecuteException("Specifica 1 o 2.");
		}
		
		try {
			WarPvP.getInstance().saveSettings();
			sender.sendMessage(ChatColor.GREEN + "Hai impostato il teletrasporto #" + args[0] + ".");
		} catch (YamlerConfigurationException e) {
			e.printStackTrace();
			throw new ExecuteException("Errore durante il salvataggio della configurazione.");
		}
	}
	
	@SubCommand("setGate")
	@SubCommandPermission(Perms.COMMAND_SETGATE)
	@SubCommandUsage("<1|2>")
	@SubCommandMinArgs(1)
	public void setGate(CommandSender sender, String label, String[] args) {
		Selection selection = WarPvP.getInstance().getWorldEdit().getSelection(CommandValidate.getPlayerSender(sender));
		CommandValidate.notNull(selection, "Non hai ancora selezionato una regione con WorldEdit.");

	    World world = selection.getWorld();
	    Vector min = selection.getNativeMinimumPoint();
	    Vector max = selection.getNativeMaximumPoint();
	    
	    CommandValidate.isTrue(min.getBlockX() == max.getBlockX() || min.getBlockZ() == max.getBlockZ(), "La selezione non può essere tridimensionale.");
	    CommandValidate.isTrue(selection.getArea() < 200, "L'area è troppo grande.");
	    
	    ConfigBlock minBlock = new ConfigBlock(min.getBlockX(), min.getBlockY(), min.getBlockZ());
	    ConfigBlock maxBlock = new ConfigBlock(max.getBlockX(), max.getBlockY(), max.getBlockZ());
		
	    GateRegion gateRegion = new GateRegion(world.getName(), minBlock, maxBlock);
		
		if (args[0].equals("1")) {
			Settings.gate1 = gateRegion;
		} else if (args[0].equals("2")) {
			Settings.gate2 = gateRegion;
		} else {
			throw new ExecuteException("Specifica 1 o 2.");
		}
		
		try {
			WarPvP.getInstance().saveSettings();
			sender.sendMessage(ChatColor.GREEN + "Hai impostato il cancello #" + args[0] + ".");
		} catch (YamlerConfigurationException e) {
			e.printStackTrace();
			throw new ExecuteException("Errore durante il salvataggio della configurazione.");
		}
	}



}
