package com.example.vachak_protoype_1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CallHelper {
    private Context context;

    public CallHelper(Context context) {
        this.context = context;
    }

    public void makePhoneCall(String contactName) {
        String phoneNumber = getPhoneNumber(contactName);
        if (phoneNumber != null) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                context.startActivity(callIntent);
            } else {
                Log.d("CallHelper", "Call permission is not granted.");
            }
        } else {
            Log.d("CallHelper", "Contact not found.");
        }
    }

    private String getPhoneNumber(String name) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    (MainActivity) context,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    102
            );
            return null;
        }

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = {"%" + name + "%"}; // Search for any match

        Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            String contactName = cursor.getString(nameIndex);
            String phoneNumber = cursor.getString(numberIndex);

            cursor.close();
            Log.d("CallHelper", "Name: " + contactName + ", Number: " + phoneNumber);
            return phoneNumber;
        }

        if (cursor != null) cursor.close();
        return null; // No contact found
    }
}
