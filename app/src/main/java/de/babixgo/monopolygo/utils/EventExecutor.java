package de.babixgo.monopolygo.utils;

import android.content.Context;
import de.babixgo.monopolygo.AccountManager;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.database.CustomerRepository;
import de.babixgo.monopolygo.database.TeamRepository;
import de.babixgo.monopolygo.models.Account;
import de.babixgo.monopolygo.models.Customer;
import de.babixgo.monopolygo.models.Team;
import java.util.List;

/**
 * Führt Event-Sequenz automatisch aus:
 * 1. Account wiederherstellen
 * 2. MonopolyGo starten
 * 3. Warten (10 Sekunden)
 * 4. Freundschaftslinks öffnen (Slot 1-4)
 * 5. Nächster Account
 */
public class EventExecutor {
    
    public interface ExecutionListener {
        void onStepComplete(String message);
        void onTeamComplete(Team team);
        void onExecutionComplete();
        void onError(String error);
    }
    
    private Context context;
    private TeamRepository teamRepository;
    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;
    private ExecutionListener listener;
    
    public EventExecutor(Context context, ExecutionListener listener) {
        this.context = context;
        this.listener = listener;
        this.teamRepository = new TeamRepository();
        this.accountRepository = new AccountRepository();
        this.customerRepository = new CustomerRepository();
    }
    
    /**
     * Führe Event für alle Teams aus
     */
    public void executeEvent(long eventId) {
        teamRepository.getTeamsByEventId(eventId)
            .thenAccept(teams -> {
                executeTeamsSequentially(teams, 0);
            })
            .exceptionally(throwable -> {
                listener.onError("Fehler beim Laden der Teams: " + throwable.getMessage());
                return null;
            });
    }
    
    private void executeTeamsSequentially(List<Team> teams, int index) {
        if (index >= teams.size()) {
            listener.onExecutionComplete();
            return;
        }
        
        Team team = teams.get(index);
        
        executeTeam(team)
            .thenRun(() -> {
                listener.onTeamComplete(team);
                // Continue with next team
                executeTeamsSequentially(teams, index + 1);
            })
            .exceptionally(throwable -> {
                listener.onError("Fehler bei Team " + team.getName() + ": " + throwable.getMessage());
                // Continue anyway
                executeTeamsSequentially(teams, index + 1);
                return null;
            });
    }
    
    /**
     * Führe ein einzelnes Team aus
     */
    private java.util.concurrent.CompletableFuture<Void> executeTeam(Team team) {
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // Get customer info
                if (team.getCustomerId() == null) {
                    throw new Exception("Team hat keinen Kunden zugewiesen");
                }
                
                Customer customer = customerRepository.getCustomerById(team.getCustomerId())
                    .get(); // Blocking call
                
                if (customer == null || customer.getFriendLink() == null) {
                    throw new Exception("Kunde-Daten nicht vollständig");
                }
                
                // Process each slot
                processSlot(team, 1, team.getSlot1AccountId(), customer);
                processSlot(team, 2, team.getSlot2AccountId(), customer);
                processSlot(team, 3, team.getSlot3AccountId(), customer);
                processSlot(team, 4, team.getSlot4AccountId(), customer);
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private void processSlot(Team team, int slotNumber, Long accountId, Customer customer) 
        throws Exception {
        
        if (accountId == null) {
            listener.onStepComplete("Slot " + slotNumber + " ist leer - überspringe");
            return;
        }
        
        // Get account
        Account account = accountRepository.getAccountById(accountId).get();
        if (account == null) {
            throw new Exception("Account nicht gefunden: ID " + accountId);
        }
        
        listener.onStepComplete("Wiederherstelle Account: " + account.getName());
        
        // 1. Force stop MonopolyGo
        AccountManager.forceStopApp();
        Thread.sleep(1000);
        
        // 2. Restore account
        String sourceFile = AccountManager.getAccountsEigenePath() + account.getName() + 
                           "/WithBuddies.Services.User.0Production.dat";
        boolean restoreSuccess = AccountManager.restoreAccount(sourceFile);
        
        if (!restoreSuccess) {
            throw new Exception("Restore fehlgeschlagen für " + account.getName());
        }
        
        listener.onStepComplete("Account wiederhergestellt: " + account.getName());
        
        // 3. Start MonopolyGo
        AccountManager.startApp();
        listener.onStepComplete("MonopolyGo gestartet - warte 10 Sekunden...");
        Thread.sleep(10000); // Wait 10 seconds
        
        // 4. Open friend link
        listener.onStepComplete("Öffne Freundschaftslink für Kunde: " + customer.getName());
        AccountManager.openFriendLink(customer.getUserId());
        Thread.sleep(2000); // Wait 2 seconds for link to open
        
        listener.onStepComplete("Slot " + slotNumber + " abgeschlossen");
    }
}
