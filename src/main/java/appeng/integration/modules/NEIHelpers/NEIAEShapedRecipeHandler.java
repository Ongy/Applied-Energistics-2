package appeng.integration.modules.NEIHelpers;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import appeng.api.exceptions.MissingIngredientError;
import appeng.api.exceptions.RegistrationError;
import appeng.api.recipes.IIngredient;
import appeng.core.AEConfig;
import appeng.recipes.game.ShapedRecipe;
import appeng.util.Platform;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.DefaultOverlayRenderer;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.api.IRecipeOverlayRenderer;
import codechicken.nei.api.IStackPositioner;
import codechicken.nei.recipe.RecipeInfo;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class NEIAEShapedRecipeHandler extends TemplateRecipeHandler
{

	public void loadTransferRects()
	{
		this.transferRects.add( new TemplateRecipeHandler.RecipeTransferRect( new Rectangle( 84, 23, 24, 18 ), "crafting", new Object[0] ) );
	}

	public Class<? extends GuiContainer> getGuiClass()
	{
		return GuiCrafting.class;
	}

	@Override
	public String getRecipeName()
	{
		return NEIClientUtils.translate( "recipe.shaped", new Object[0] );
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results)
	{
		if ( (outputId.equals( "crafting" )) && (getClass() == NEIAEShapedRecipeHandler.class) )
		{
			List<IRecipe> allrecipes = CraftingManager.getInstance().getRecipeList();
			for (IRecipe irecipe : allrecipes)
			{
				if ( (irecipe instanceof ShapedRecipe) )
				{
					if ( ((ShapedRecipe) irecipe).isEnabled() )
					{
						CachedShapedRecipe recipe = new CachedShapedRecipe( (ShapedRecipe) irecipe );
						recipe.computeVisuals();
						arecipes.add( recipe );
					}
				}
			}
		}
		else
		{
			super.loadCraftingRecipes( outputId, results );
		}
	}

	public void loadCraftingRecipes(ItemStack result)
	{
		List<IRecipe> allrecipes = CraftingManager.getInstance().getRecipeList();
		for (IRecipe irecipe : allrecipes)
		{
			if ( (irecipe instanceof ShapedRecipe) )
			{
				if ( ((ShapedRecipe) irecipe).isEnabled() && NEIServerUtils.areStacksSameTypeCrafting( irecipe.getRecipeOutput(), result ) )
				{
					CachedShapedRecipe recipe = new CachedShapedRecipe( (ShapedRecipe) irecipe );
					recipe.computeVisuals();
					arecipes.add( recipe );
				}
			}
		}
	}

	public void loadUsageRecipes(ItemStack ingredient)
	{
		List<IRecipe> allrecipes = CraftingManager.getInstance().getRecipeList();
		for (IRecipe irecipe : allrecipes)
		{
			if ( (irecipe instanceof ShapedRecipe) )
			{
				CachedShapedRecipe recipe = new CachedShapedRecipe( (ShapedRecipe) irecipe );

				if ( ((ShapedRecipe) irecipe).isEnabled() && recipe.contains( recipe.ingredients, ingredient.getItem() ) )
				{
					recipe.computeVisuals();
					if ( recipe.contains( recipe.ingredients, ingredient ) )
					{
						recipe.setIngredientPermutation( recipe.ingredients, ingredient );
						arecipes.add( recipe );
					}
				}
			}
		}
	}

	public String getGuiTexture()
	{
		return "textures/gui/container/crafting_table.png";
	}

	public String getOverlayIdentifier()
	{
		return "crafting";
	}

	@Override
	public boolean hasOverlay(GuiContainer gui, Container container, int recipe)
	{
		return (super.hasOverlay( gui, container, recipe )) || ((isRecipe2x2( recipe )) && (RecipeInfo.hasDefaultOverlay( gui, "crafting2x2" )));
	}

	@Override
	public IRecipeOverlayRenderer getOverlayRenderer(GuiContainer gui, int recipe)
	{
		IRecipeOverlayRenderer renderer = super.getOverlayRenderer( gui, recipe );
		if ( renderer != null )
			return renderer;

		IStackPositioner positioner = RecipeInfo.getStackPositioner( gui, "crafting2x2" );
		if ( positioner == null )
			return null;

		return new DefaultOverlayRenderer( getIngredientStacks( recipe ), positioner );
	}

	@Override
	public IOverlayHandler getOverlayHandler(GuiContainer gui, int recipe)
	{
		IOverlayHandler handler = super.getOverlayHandler( gui, recipe );
		if ( handler != null )
			return handler;

		return RecipeInfo.getOverlayHandler( gui, "crafting2x2" );
	}

	public boolean isRecipe2x2(int recipe)
	{
		for (PositionedStack stack : getIngredientStacks( recipe ))
		{
			if ( (stack.relx > 43) || (stack.rely > 24) )
				return false;
		}
		return true;
	}

	public class CachedShapedRecipe extends TemplateRecipeHandler.CachedRecipe
	{

		public ArrayList<PositionedStack> ingredients;
		public PositionedStack result;

		public CachedShapedRecipe(ShapedRecipe irecipe) {
			result = new PositionedStack( irecipe.getRecipeOutput(), 119, 24 );
			ingredients = new ArrayList<PositionedStack>();
			setIngredients( irecipe.getWidth(), irecipe.getHeight(), irecipe.getIngredients() );
		}

		public void setIngredients(int width, int height, Object[] items)
		{
			boolean useSingleItems = AEConfig.instance.disableColoredCableRecipesInNEI();
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					if ( items[(y * width + x)] != null )
					{
						IIngredient ing = (IIngredient) items[(y * width + x)];

						try
						{
							ItemStack[] is = ing.getItemStackSet();
							PositionedStack stack = new PositionedStack( useSingleItems ? Platform.findPreferred( is ) : is, 25 + x * 18, 6 + y * 18, false );
							stack.setMaxSize( 1 );
							this.ingredients.add( stack );
						}
						catch (RegistrationError e)
						{

						}
						catch (MissingIngredientError e)
						{

						}

					}
				}
			}
		}

		@Override
		public List<PositionedStack> getIngredients()
		{
			return getCycledIngredients( cycleticks / 20, this.ingredients );
		}

		@Override
		public PositionedStack getResult()
		{
			return this.result;
		}

		public void computeVisuals()
		{
			for (PositionedStack p : this.ingredients)
			{
				p.generatePermutations();
			}
			this.result.generatePermutations();
		}
	}
}