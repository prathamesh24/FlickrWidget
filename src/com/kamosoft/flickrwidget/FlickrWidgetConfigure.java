/**
 * Copyright 2011 kamosoft
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kamosoft.flickrwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.kamosoft.flickr.FlickrConnect;
import com.kamosoft.flickrwidget.WidgetConfiguration.Content;

/**
 * The widget configuration activity.
 * Perform the flickr authentification and other stuff
 * @author Tom
 * created 10 mars 2011
 */
public class FlickrWidgetConfigure
    extends Activity
    implements View.OnClickListener
{
    static final int DIALOG_NO_NETWORK = 1;

    private int mAppWidgetId;

    private static final int AUTHENTICATE = 0;

    private RadioButton mRadioButtonUserPhotos;

    private RadioButton mRadioButtonUserComments;

    private Button mCommitButton;

    private FlickrConnect mFlickrConnect;

    //private SharedPreferences mFlickrLibraryPrefs;

    /**
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        Log.d( "FlickrWidgetConfigure: Start onCreate" );
        /* set the Activity result to RESULT_CANCELED. 
         * This way, if the user backs-out of the Activity before reaching the end, 
         * the App Widget host is notified that the configuration was cancelled and the App Widget will not be added. */
        setResult( RESULT_CANCELED );

        mFlickrConnect = new FlickrConnect( this );

        /* retrieve the widget id */
        mAppWidgetId = getIntent().getExtras().getInt( AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                       AppWidgetManager.INVALID_APPWIDGET_ID );
        if ( mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID )
        {
            Log.e( "FlickrWidgetConfigure: Error bad appWidgetId" );
            finish();
            return;
        }
        Log.d( "FlickrWidgetConfigure: widget app id = " + mAppWidgetId );

        Log.d( "FlickrWidgetConfigure: Checking auth" );

        /* check the authentification */
        mFlickrConnect.isLoggedIn( new FlickrConnect.LoginHandler()
        {

            @Override
            public void onLoginSuccess()
            {
                Log.d( "FlickrWidgetConfigure: Auth OK" );
                /* auth OK, we display the configuration layout */
                displayConfigureLayout();
            }

            @Override
            public void onLoginFailed()
            {
                Log.d( "FlickrWidgetConfigure: Auth fail" );
                /* auth need to be done, we display the connect button */
                setContentView( R.layout.connect );
            }

            @Override
            public void onLoginError()
            {
                Log.d( "FlickrWidgetConfigure: Auth error" );
                Toast.makeText( FlickrWidgetConfigure.this, "Network error", Toast.LENGTH_SHORT ).show();
                FlickrWidgetConfigure.this.finish();

            }
        } );

        Log.d( "FlickrWidgetConfigure: End onCreate" );
    }

    /**
     * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged( Configuration newConfig )
    {
        super.onConfigurationChanged( newConfig );
    }

    /**
     * display the configuration layout, fill some attributes and textviews.
     */
    private void displayConfigureLayout()
    {
        setContentView( R.layout.widget_configure );
        mRadioButtonUserComments = (RadioButton) findViewById( R.id.radioButton_userComments );
        mRadioButtonUserComments.setOnClickListener( this );
        mRadioButtonUserPhotos = (RadioButton) findViewById( R.id.radioButton_userPhotos );
        mRadioButtonUserPhotos.setOnClickListener( this );
        mCommitButton = (Button) findViewById( R.id.button_commit );

        /* display the userName */
        String userName = mFlickrConnect.getFlickrParameters().getUserName();
        Button button = (Button) findViewById( R.id.connected_to_flickr );
        button.setText( getString( R.string.connected_to_flickr, userName ) );
    }

    /**
     * Starts the flickr authentification
     * @param view
     */
    public void onConnect( View view )
    {
        mFlickrConnect.authorize( this, getString( R.string.app_name ), getString( R.string.api_key ),
                                  getString( R.string.api_secret ), getString( R.string.auth_url ), AUTHENTICATE );
    }

    /**
     * Disconnect from Flickr
     * @param view
     */
    public void onDisconnect( View view )
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.alert_disconnect ).setCancelable( false )
            .setPositiveButton( R.string.yes, new DialogInterface.OnClickListener()
            {
                public void onClick( DialogInterface dialog, int id )
                {
                    Toast.makeText( FlickrWidgetConfigure.this, R.string.disconnect_ok, Toast.LENGTH_SHORT ).show();
                    mFlickrConnect.logOut();
                    FlickrWidgetConfigure.this.setContentView( R.layout.connect );
                }
            } ).setNegativeButton( R.string.no, new DialogInterface.OnClickListener()
            {
                public void onClick( DialogInterface dialog, int id )
                {
                    dialog.cancel();
                }
            } );
        builder.create().show();
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        super.onActivityResult( requestCode, resultCode, data );
        Log.d( "FlickrWidgetConfigure: Start onActivityResult" );
        switch ( requestCode )
        {
            case AUTHENTICATE:
                if ( resultCode == FlickrConnect.AUTH_SUCCESS )
                {
                    Toast.makeText( this, R.string.connectOK, Toast.LENGTH_SHORT ).show();
                    displayConfigureLayout();
                }
                else
                {
                    Toast.makeText( this, R.string.connectKO, Toast.LENGTH_SHORT ).show();
                }
                break;
        }
        Log.d( "FlickrWidgetConfigure: end onActivityResult" );
    }

    /**
     * @param context
     * @param appWidgetId
     * @return
     */
    public static WidgetConfiguration loadConfiguration( Context context, int appWidgetId )
    {
        SharedPreferences widgetPrefs = context.getSharedPreferences( Constants.WIDGET_PREFS, 0 );
        WidgetConfiguration widgetConfiguration = new WidgetConfiguration();
        widgetConfiguration.setContent( widgetPrefs.getString( Constants.WIDGET_CONTENT + appWidgetId, null ) );

        widgetConfiguration.setMaxItems( widgetPrefs.getInt( Constants.WIDGET_MAXITEMS + appWidgetId, 10 ) );
        return widgetConfiguration;
    }

    /**
     * @param context
     * @param appWidgetId
     * @return
     */
    public static void clearConfiguration( Context context, int appWidgetId )
    {
        SharedPreferences widgetPrefs = context.getSharedPreferences( Constants.WIDGET_PREFS, 0 );
        Editor editor = widgetPrefs.edit();
        editor.remove( Constants.WIDGET_CONTENT + appWidgetId );
        editor.remove( Constants.WIDGET_MAXITEMS + appWidgetId );
        editor.commit();
    }

    /**
     * @param context
     * @param appWidgetId
     * @return
     */
    public static void saveConfiguration( Context context, int appWidgetId, WidgetConfiguration widgetConfiguration,
                                          int maxItems )
    {
        SharedPreferences widgetPrefs = context.getSharedPreferences( Constants.WIDGET_PREFS, 0 );
        Editor editor = widgetPrefs.edit();
        editor.putString( Constants.WIDGET_CONTENT + appWidgetId,
                          widgetConfiguration.getContent() == null ? null : widgetConfiguration.getContent().name() );
        editor.putInt( Constants.WIDGET_MAXITEMS + appWidgetId, maxItems );
        editor.commit();
    }

    /**
     * @return the max number of item to be retrieved from the flickrAPI
     * Depends on the widget size so it is the subclasses FlickrWidgetConfigure4X4, FlickrWidgetConfigure4X3...
     * which implements this methods
     */
    protected int getMaxItems()
    {
        throw new UnsupportedOperationException( "this method need to be overrided" );
    }

    public WidgetConfiguration generateWidgetConfiguration()
    {
        WidgetConfiguration widgetConfiguration = new WidgetConfiguration();
        if ( mRadioButtonUserComments.isChecked() )
        {
            widgetConfiguration.setContent( Content.userComments );
        }
        else
        {
            widgetConfiguration.setContent( Content.userPhotos );
        }
        return widgetConfiguration;
    }

    public void onConfigurationDone( View view )
    {
        /* save the configuration */
        WidgetConfiguration widgetConfiguration = generateWidgetConfiguration();
        saveConfiguration( this, mAppWidgetId, widgetConfiguration, getMaxItems() );

        /* now the widget need to be manually updated */
        Log.d( "FlickrWidgetConfigure: Start Widget update with id" + mAppWidgetId );

        WidgetUpdateService.start( this, mAppWidgetId );

        Intent resultValue = new Intent();
        resultValue.putExtra( AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId );
        setResult( RESULT_OK, resultValue );
        Log.d( "FlickrWidgetConfigure: Widget updated" );
        finish();
    }

    public void onCancel( View view )
    {
        finish();
    }

    public void onAbout( View view )
    {
        new AboutDialog( this ).show();
    }

    private boolean isConfigurationOk()
    {
        return mRadioButtonUserPhotos.isChecked() || mRadioButtonUserComments.isChecked();
    }

    /**
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick( View v )
    {
        mCommitButton.setEnabled( isConfigurationOk() );
    }

    protected Dialog onCreateDialog( int id )
    {
        AlertDialog.Builder builder;
        switch ( id )
        {
            case DIALOG_NO_NETWORK:
                builder = new AlertDialog.Builder( this );
                builder.setMessage( R.string.no_network ).setTitle( R.string.error )
                    .setIcon( android.R.drawable.ic_dialog_alert )
                    .setNeutralButton( "OK", new DialogInterface.OnClickListener()
                    {
                        public void onClick( DialogInterface dialog, int id )
                        {
                            dialog.dismiss();
                            FlickrWidgetConfigure.this.finish();
                        }
                    } );
                return builder.create();

        }
        return null;
    }
}
