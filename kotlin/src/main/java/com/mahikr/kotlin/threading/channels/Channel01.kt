@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mahikr.kotlin.threading.channels

import android.util.Log
import com.mahikr.kotlin.threading.coroutines.AppDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/******Channels
 * Channels used for the asynchronous communication between coroutines.
 * allow to pass stream of value from one coroutine to another.
 * Elements will be processed in same order it has been sent
 *
 * ex: assume it as pipe
 * one coroutine send a data (producer)
 * another coroutine consumes the data (consumer/receiver)
 *
 * channels can have multiple receiver, but data received once by the channel
 *
 *Channel:
 * Channel help to handle the hot stream data.
 *
 * send and receive is the api to send and receive the data send through the channel.
 *
 * val channel = Channel<Type>()
 * channel.send(data)
 * channel.receive()
 *
 * it need to close manually
 * channel.close()
 *
 * channel.isClosedForReceive
 * channel.isClosedForSend
 * channel.invokeOnClose{
 *  ... to handle the clean-up activity....
 * }
 *
 *
 * AutoCloser
 * var channel5:ReceiveChannel<Int> = Channel<Int>()
 *
 * //auto closer on the sender
 * channel = produce{
 *  send()
 * }
 *
 * channel.consumeEach{
 *  //loop through the channel until the channel is empty
 * }
 * channel.isClosedForSend
 *
 * //there is no invokeOnClose api.
 *
 * types of channel:
 * Channels are classified based on the buffer or capacity it can hold on the sender site.
 * capacity: indicates that the max capacity of the channel to send the element until it consumed and space for the sender buffer is freed up it can't send new element
 *    ex:capacity is 2 only two element can be send by the channel, if one of that element is collected or received then next element can be send through it
 *  types are:
 *  1. RENDEZVOUS(default), capacity = 0, onBufferOverflow = BufferOverflow.SUSPEND. ie. it'll be suspended till the sender site gets capacity
 *  2. CONFLATED, capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST. ie. it'll drop the oldest data if there is no space at the sender site gets capacity
 *  3. UNLIMITED, capacity = unlimited,
 *  4. BUFFERED, capacity = allow to specify the buffer ie. capacity,
 *
 *
 * //RENDEZVOUS,CONFLATED,UNLIMITED,BUFFERED
 * //how many element it can store internally until the value been received
 * //BUFFERED: allow max capacity. until the value been consumed and buffer capacity been freed. it can't send a new value.
 * //capacity: indicates that the max capacity of the channel to send the element until it consumed and space for the sender buffer is freed up it can't send new element
 *
 */

fun log(mes: Any?) {
    println("[${Thread.currentThread().name}]: $mes")
}

@OptIn(DelicateCoroutinesApi::class)
private suspend fun main() {
    log("Entry main")
    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        log("handler >> CoroutineExceptionHandler >> $coroutineContext\nmessage-> ${throwable.message}\nprintStackTrace-> ${throwable.printStackTrace()}")
    }
    var scope: CoroutineScope? =
        CoroutineScope(AppDispatcher.default + SupervisorJob() + CoroutineName("Channel_Coroutine01") + handler)
    scope?.launch {


        coroutineScope {
            val channel = Channel<Int>()
            launch {
                log("Producer end ${channel.isClosedForReceive} ${channel.isClosedForSend}")
                channel.send(10)
                log("sent 10 ${channel.isClosedForReceive} ${channel.isClosedForSend}")
                //delay(500L) //not able to send the data
                //log("Receiver end ${channel.isClosedForReceive} ${channel.isClosedForSend}")
                //val data = channel.receive()
                //log("receive $data ${channel.isClosedForReceive} ${channel.isClosedForSend}")*/
            }

            launch {
                delay(500L)
                log("Receiver end ${channel.isClosedForReceive} ${channel.isClosedForSend}")
                channel.receive().also { data: Int ->
                    log("receive $data ${channel.isClosedForReceive} ${channel.isClosedForSend}")
                }
            }
        }


        coroutineScope {
            val channel: Channel<Int> = Channel()
            launch {
                for (i in 1..10 step 3) {
                    log("Producing $i")
                    channel.send(i)
                }
                //for consumeEach to close, it needs to be notified of producer close....
                channel.close()
            }

            launch {
                channel.consumeEach {
                    log("consuming $it")
                }
            }
        }

    }.also { job ->
        job?.invokeOnCompletion {
            scope = null
            log("Entry invokeOnCompletion $scope message: ${it?.message}")
        }
    }?.join()

    scope = CoroutineScope(AppDispatcher.default)
    scope?.launch {
        var channel:Channel<LANGUAGES> = Channel()
        launch {
            log("kotlin sent")
            channel.send(LANGUAGES.KOTLIN)
            log("java sent")
            channel.send(LANGUAGES.JAVA)
            channel.close()
        }
        launch {
            //receive only able to receive one element on invocation
            channel.receive().also { log("recived $it") }
            delay(1000L)
            channel.receive().also { log("recived $it") }
            channel.consumeEach {  log("recived $it") }
            channel.invokeOnClose {
                log("channel closed")
            }
        }
    }?.join()
    log("Exit main")
}

enum class LANGUAGES {
    KOTLIN, JAVA, PYTHON, JAVA_SCRIPT
}


class IntReceiveChannel(){
    private var intReceiveChannel:ReceiveChannel<Int> = Channel()
    suspend fun produceInt(){
        withContext(Dispatchers.Default){
            intReceiveChannel = produce(capacity = 4) {
                Log.d("TAGi", "send(1)")
                trySend(1)
                delay(1000L)
                Log.d("TAGi", "send(2)")
                send(2)
                Log.d("TAGi", "send(3)")
                trySend(3)
                Log.d("TAGi", "send(4)")
                send(4)
                Log.d("TAGi", "send(5)")
                send(5)
                Log.d("TAGi", "send(6)")
                send(6)
                Log.d("TAGi", "send(7)")
                send(7)
            }
        }
    }

    suspend fun consumeInt(){
        withContext(Dispatchers.Default){
            /*channel.consumeEach {
                       Log.d("TAGi", "consumeEach: $it")
            }*/
            intReceiveChannel.consumeAsFlow().onEach { delay(100L)
                Log.d("TAGi", "consumeAsFlow:$it ") }.launchIn(this)
        }
    }




}