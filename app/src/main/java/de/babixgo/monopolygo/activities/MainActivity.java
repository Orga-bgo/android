package de.babixgo.monopolygo.activities;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.fragments.AccountListFragment;
import de.babixgo.monopolygo.fragments.TycoonRacersFragment;
import de.babixgo.monopolygo.fragments.CustomerManagementFragment;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Setup Hamburger-Icon
        toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup Navigation
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        // Load default fragment (AccountListFragment)
        if (savedInstanceState == null) {
            loadFragment(new AccountListFragment());
            navigationView.setCheckedItem(R.id.nav_accounts);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Accountliste");
            }
        }
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        String title = "";

        if (item.getItemId() == R.id.nav_accounts) {
            fragment = new AccountListFragment();
            title = "Accountliste";
        } else if (item.getItemId() == R.id.nav_tycoon_racers) {
            fragment = new TycoonRacersFragment();
            title = "Tycoon Racers";
        } else if (item.getItemId() == R.id.nav_customers) {
            fragment = new CustomerManagementFragment();
            title = "Kunden";
        }

        if (fragment != null) {
            loadFragment(fragment);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
