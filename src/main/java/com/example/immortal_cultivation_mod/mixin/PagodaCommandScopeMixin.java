package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.pagoda.PagodaCommandScope;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Commands.class)
public abstract class PagodaCommandScopeMixin {
    @WrapMethod(method="performCommand")
    private void pagoda$adminCommands(ParseResults<CommandSourceStack> parse,String command,Operation<Void> original){
        boolean old=PagodaCommandScope.set(parse.getContext().getSource().hasPermission(2));
        try{original.call(parse,command);}finally{PagodaCommandScope.set(old);}
    }
}
