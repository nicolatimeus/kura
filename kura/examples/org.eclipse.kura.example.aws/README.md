Eclipse Kura can connect to the Amazon AWS IoT Platform using the MQTT protocol. When doing so, Kura applications can send device-to-cloud and receive cloud-to-device messages.

A guide describing how to connect a Kura device to AWS IoT is available in the Kura documentation.

This example application demonstrates how to send and receive messages from/to the AWS IoT platform and how to use the device shadow.

# Used MQTT topics

This application will publish periodic messages on the following topic:

  `{account-id}/{client-id}/{app-id}/{publish-topic}`

  * **{account-id}** is the value of the configuration setting **topic.context.account-name** in the configuration of the AWS CloudService instance.
  * **{client-id}** is the value of the configuration setting **client-id** in the configuration of the AWS CloudService instance.
  * **{app-id}** is the value of the configuration setting **app.name** in the configuration of this application.
  * **{publish-topic}** is the value of the configuration setting **publish.topic** in the configuration of this application.

  The default value is `{account-id}/{client-id}/messages/events`



  This application subscribes to the following topic and prints on the log the received messages:

`{account-id}/{client-id}/{app-id}/{publish-topic}`

  * **{account-id}**, **{client-id}** and **{app-id}** are defined like above.
  * **{publish-topic}** is the value of the configuration setting **publish.topic** in the configuration of this application

  The default value is `{account-id}/{client-id}/messages/inbound`

# Interacting with the device

It is possible to view published data and send messages to the device directly from the browser performing the following steps:

  1. Connect a Kura device to AWS IoT and install this example application.

  2. Access the AWS IoT web console.

  3. Click on **Test** in the left side menu.

  4. Enter the publishing topic described above in the **Subscription topic** field and click on the **Subscribe to topic** button. It should be possible to see the payload of the published messages by clicking on the topic name under **Subscriptions**.

  5. Enter the value of the topic used for the subscription by the device in the text field near **Publish**. The received messages should be printed on the log by the device.

# Using the Device Shadow

The Device Shadow can be used to report the device status or for remote configuration, for more information see [http://docs.aws.amazon.com/iot/latest/developerguide/iot-thing-shadows.html](http://docs.aws.amazon.com/iot/latest/developerguide/iot-thing-shadows.html).

The Device Shadow is a JSON document containing the following properties:

  * *reported* is a JSON object updated by the device to report its status/configuration.

  * *desired* is a JSON object that can be updated using the REST APIs to request a configuration/status change to the device.

This application demonstrates how to use the shadow by implementing a simple echo service. It can be tested performing the following steps:

  1. Connect a Kura device to AWS IoT and install this example application.

  2. Access the AWS IoT web console.

  3. Enter the configuration section for the newly connected device (click on **Registry** -> **Things** in the left panel and then on the device name).

  4. Click on **Shadow** on the left side of the screen, the following Shadow state should be reported:

   ```
   {
      "reported": {
        "echo": "modify this property in the desired shadow"
      }
   }
   ```

  5. Modify the shadow state as follows:

   ```
   {
      "reported": {
        "echo": "modify this property in the desired shadow"
      },
      "desired": {
        "echo": "test string"
      }
   }
   ```

  6. After the modification the shadow state should look as follows:

   ```
  {
      "reported": {
        "echo": "test string"
      },
      "desired": {
        "echo": "test string"
      }
  }
  ```

  Checking the device logs it should be possible to find some messages reporting that the device acknowledged the shadow changes, and that it updated the *echo* property of the *reported* shadow to match the *desired* shadow.
