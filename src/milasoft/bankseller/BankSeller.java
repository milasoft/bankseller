package milasoft.bankseller;

import java.util.ArrayList;
import java.util.List;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.bank.BankMode;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.items.Item;

import milasoft.bankseller.prices.Prices;

@ScriptManifest(author = "Milasoft", category = Category.MISC, name = "Bank Seller", version = 1.0)
public class BankSeller extends AbstractScript {

	final Area GRAND_EXCHANGE = new Area(3160, 3493, 3168, 3486);
	List<Item> itemList;
	List<String> wontSell;
	Prices prices;
	
	boolean shutdown;
	int slots;
	
	@Override
	public void onStart() {
		prices = new Prices();
		wontSell = new ArrayList<String>();
		wontSell.add("Coins");
	}
	
	@Override
	public int onLoop() {
		if(slots == 0) {
			if(getWorlds().getWorld(getClient().getCurrentWorld()).isMembers()) {
				slots = 8;
			} else {
				slots = 3;
			}
		}
		if(shutdown) {
			log("Out of items, shutting down.");
			if(!getInventory().isEmpty()) {
				if(getBank().isOpen()) {
					getBank().depositAllItems();
					sleepUntil(() -> getInventory().isEmpty(), 2500);
					getBank().close();
				} else {
					getBank().open();
					sleepUntil(() -> getBank().isOpen(), 2500);
				}
			} else {
				stop();
			}
		} else {
			if(GRAND_EXCHANGE.contains(getLocalPlayer())){
				if(shouldWithdraw()) {
					withdrawItem();
				} else {
					sellItem();
				}
			} else {
				if(getWalking().shouldWalk(Calculations.random(6, 10))) {
					getWalking().walk(GRAND_EXCHANGE.getRandomTile());
				}
			}
		}
		return Calculations.random(250, 400);
	}
	
	void withdrawItem() {
		if(getBank().isOpen()) {
			if(getInventory().isFull()) {
				closeBank();
				return;
			}
			for(Item i : getInventory().all(i -> i != null && wontSell.contains(i.getName()))) {
				getBank().depositAll(i.getID());
				sleepUntil(() -> !getInventory().contains(i.getID()), 750);
			}
			/* This could take a while... it is caching every item in your bank and checking the price. */
			if(itemList == null) {
				if(getWorlds().getWorld(getClient().getCurrentWorld()).isMembers()) {
					itemList = getBank().all(i -> i != null && isSellable(i.getID()));
				} else {
					itemList = getBank().all(i -> i != null && isSellable(i.getID()) && !i.isMembersOnly());
				}
				itemList.sort((i, i2) -> Integer.compare(getBank().slot(i.getID()), getBank().slot(i.getID())));
			} else {
				if(itemList.isEmpty()) {
					closeBank();
					return;
				}
			}
			if(getBank().getWithdrawMode() != BankMode.NOTE) {
				getBank().setWithdrawMode(BankMode.NOTE);
			}
			Item item = itemList.get(0);
			if(item != null) {
				if(getBank().needToScroll(item)) {
					getBank().scroll(item.getID());
				}
				if(item.getAmount() > 1) {
					if(getBank().withdrawAll(item.getID())) {
						itemList.remove(0);
					}
				} else {
					if(getBank().withdraw(item.getID())) {
						itemList.remove(0);
					}
				}
				if(getInventory().isFull()) {
					closeBank();
				}
			} else {
				closeBank();
			}
		} else {
			getBank().open();
			sleepUntil(() -> getBank().isOpen(), 2500);
		}
	}
	
	void sellItem() {
		if(getBank().isOpen()) {
			closeBank();
		}
		if(getGrandExchange().isOpen()) {
			if(!getInventory().contains(i -> i != null && !wontSell.contains(i.getName()))) {
				cancelOffers();
				if(itemList.isEmpty()) {
					shutdown = true;
				}
				getGrandExchange().close();
				sleepUntil(() -> !getGrandExchange().isOpen(), 2000);
			}
			if(getGrandExchange().getFirstOpenSlot() != -1) {
				if(getInventory().contains(i -> i != null && !wontSell.contains(i.getName()))) {
					Item item = getInventory().get(i -> i != null && !wontSell.contains(i.getName()));
					getGrandExchange().sellItem(item.getName(), item.getAmount(), 1);
					sleep(350, 750);
				} else {
					if(getGrandExchange().isReadyToCollect()) {
						getGrandExchange().collect();
					}
				}
			} else {
				cancelOffers();
			}
		} else {
			getGrandExchange().open();
			sleepUntil(() -> getGrandExchange().isOpen(), 2500);
		}
	}
	
	boolean isSellable(int itemId) {
		return prices.getPrice(itemId) > 0;
	}
	
	void closeBank() {
		getBank().close();
		sleepUntil(() -> !getBank().isOpen(), 2500);
	}
	
	void cancelOffers() {
		for(int i = 0; i < slots; i++) {
			if(getGrandExchange().slotContainsItem(i)) {
				sleep(250, 500);
				if(!getGrandExchange().isReadyToCollect(i)) {
					wontSell.add(getGrandExchange().getItems()[i].getName());
					getGrandExchange().cancelOffer(i);
					getGrandExchange().goBack();
				}
			}
		}
		if(getGrandExchange().isReadyToCollect()) {
			getGrandExchange().collect();
		}
	}
	
	boolean shouldWithdraw() {
		if(getGrandExchange().isOpen() || getInventory().isFull()) {
			return false;
		}
		return getInventory().isEmpty() || getBank().isOpen() ||
			   getInventory().onlyContains(i -> i != null && wontSell.contains(i.getName()));
	}
}
