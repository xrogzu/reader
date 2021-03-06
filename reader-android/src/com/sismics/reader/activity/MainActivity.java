package com.sismics.reader.activity;

import org.json.JSONObject;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import com.sismics.android.SismicsHttpResponseHandler;
import com.sismics.reader.R;
import com.sismics.reader.fragment.ArticlesFragment;
import com.sismics.reader.model.application.ApplicationContext;
import com.sismics.reader.resource.SubscriptionResource;
import com.sismics.reader.resource.UserResource;
import com.sismics.reader.ui.adapter.SubscriptionAdapter;
import com.sismics.reader.ui.adapter.SubscriptionAdapter.SubscriptionItem;

/**
 * Main activity.
 * 
 * @author bgamard
 */
public class MainActivity extends FragmentActivity {
    
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    
    @Override
    protected void onCreate(final Bundle args) {
        super.onCreate(args);

        // Check if logged in
        if (!ApplicationContext.getInstance().isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main_activity);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.drawer_list);
        drawerList.setEmptyView(findViewById(R.id.progressBarDrawer));

        // Set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        
        // Load subscriptions and select unread item
        // TODO Display cached subscriptions before loading fresh data from server
        if (args == null) {
            refreshSubscriptions(1, false);
        } else {
            refreshSubscriptions(args.getInt("drawerItemSelected", 1), false);
        }

        // Drawer item click listener
        drawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position, false);
            }
        });
        
        // Enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.logout:
            UserResource.logout(getApplicationContext(), new SismicsHttpResponseHandler() {
                @Override
                public void onFinish() {
                    // Force logout in all cases, so the user is not stuck in case of network error
                    ApplicationContext.getInstance().setUserInfo(getApplicationContext(), null);
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            });
            return true;
            
//        case R.id.settings:
//            startActivity(new Intent(NavigationActivity.this, SettingsActivity.class));
//            return true;
            
        case R.id.about:
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return true;
            
        case R.id.refresh:
            // Refresh subscriptions and articles
            refreshSubscriptions(drawerList.getCheckedItemPosition(), true);
            return true;
            
        case android.R.id.home:
            // The action bar home/up action should open or close the drawer.
            // ActionBarDrawerToggle will take care of this.
            if (drawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
            return true;
            
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Select an item from the subscription list.
     * @param position Position to select
     * @param refresh True to force articles refresh
     */
    private void selectItem(int position, boolean refresh) {
        // Create a new fragment with articles context
        SubscriptionAdapter adapter = (SubscriptionAdapter) drawerList.getAdapter();
        if (adapter == null) {
            return;
        }
        
        SubscriptionItem item = adapter.getItem(position);
        if (item == null) {
            return;
        }
        
        Fragment fragment = new ArticlesFragment();
        Bundle args = new Bundle();
        args.putString("url", item.getUrl());
        args.putBoolean("unread", item.isUnread());
        fragment.setArguments(args);

        // Update the main content by replacing fragment if it has different arguments from the previous one
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment oldFragment = fragmentManager.findFragmentByTag("articlesFragment");
        
        boolean replace = true;
        if (oldFragment != null && !refresh) {
            Bundle oldArgs = oldFragment.getArguments();
            if (oldArgs != null) {
                if (args.getString("url").equals(oldArgs.getString("url"))
                        && args.getBoolean("unread") == oldArgs.getBoolean("unread")) {
                    replace = false;
                }
            }
        }
        
        if (replace) {
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, "articlesFragment").commitAllowingStateLoss();
        }

        // Update selected item and title, then close the drawer
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }

    /**
     * Refresh subscriptions list from server.
     * @param position Position to select
     * @param refresh True to force articles refresh
     */
    private void refreshSubscriptions(final int position, final boolean refresh) {
        // Load subscriptions from server
        SubscriptionResource.list(this, false, new SismicsHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject json) {
                // Update or create adapter
                SubscriptionAdapter adapter = (SubscriptionAdapter) drawerList.getAdapter();
                if (adapter == null) {
                    adapter = new SubscriptionAdapter(MainActivity.this, json);
                    drawerList.setAdapter(adapter);
                } else {
                    adapter.setItems(json);
                    adapter.notifyDataSetChanged();
                }
                
                if (position != -1) {
                    int pos = position;
                    // Check if item exists and is selectable
                    if (!adapter.isEnabled(pos)) {
                        pos = 1;
                    }
                    selectItem(pos, refresh);
                }
            }
        });
    }
    
    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        drawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("drawerItemSelected", drawerList.getCheckedItemPosition());
    }
}