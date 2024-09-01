package com.mahikr.kotlinnotes.threading.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

/***** Coroutine Exception Handling
 *  If Exception not handled directly using try & catch block, it'll propagate up towards root scope.
 *    if Exceptions aren't handled or coroutineExceptionHandler not installed
 *      General Exception: cause app to crash
 *      Cancellation Exception: won't cause app to crash, since it'll be handled internally.
 *
 * Exception propagation & throw => cause app to crash
 *  based on the root coroutineBuilderType:
 *      launch: ASAP app launched
 *      async: accumulates the exception and on invocation of await exception will be thrown.
 *
 *  General exception handling:
 *     eaten-up the CancellationException and won't propagate the it to it's parent.
 *     solution:
 *      1. Do Specific Exception handling.
 *      2. Re-throw Cancellation Exception
 *      3. make use of CoroutineContext.
 *
 * coroutineExceptionHandler:
 *    -> can only be installed @root coroutine.
 *    -> will catch uncaught exceptions.
 *    -> cancellation exception will not be caught by it. since it'll be handled internally by coroutine.
 *    -> Make use of coroutineContext.ensureActive()/isActive in loop
 *    -> Divide Larger code into small chunk and check cancellation @each suspension point.
 *
 *  Scope:
 *      coroutineScope vs superVisorScope:
 *          superVisorScope:
 *             *. Each coroutine inside superVisorScope will be treated as root coroutine
 *             *. If one of the child coroutine catches exception[Non-Cancellation Exception]
 *                only that specific coroutine will be cancelled.
 *                No sibling coroutines harmed/stopped by it.
 *             *. Cancellation exception, if it was caught and re-thrown/uncaught
 *                then specific coroutine will be cancelled not siblings
 *
 *          coroutineScope:
 *             *. If one of the child coroutine catches exception[Non-Cancellation Exception]
 *                terminates all coroutines including all sibling coroutines.
 *             *. Cancellation exception, if it was caught and re-thrown/uncaught
 *                then specific coroutine will be cancelled not siblings
 *
 *
 *  SupervisorJob vs without SupervisorJob
 *      while creating the custom scope:
 *      SupervisorJob vs without SupervisorJob is same as superVisorScope vs coroutineScope:
 *      ex:
 *          var scope :CoroutineScope?= CoroutineScope(AppDispatcher.default + exceptionHandler+ SupervisorJob())
 *          make sure to clean it up on completion
 *
 *          scope?.launch {
 *                  exceptionHandlingMain02()
 *          }.also {
 *                  it?.invokeOnCompletion {
 *                      scope = null
 *                      logger.log("clean-up scope $scope")
 *                 }
 *          }?.join()
 *
 *
 * Note:
 * Canceling parent coroutine will cancels all of it's child to get terminate.
 * If Child coroutine catches exception, it'll propagate up towards parent,
 *  if scope is supervisorScope then that specific coroutine will be cancelled, no harm for siblings.
 *  if exception handler not installed directly then that uncaught exception will be caught by actual root coroutine.
 *  if scope is coroutineScope then it cancels child coroutines and propagates the exception up towards it's parent.
 *
 *
 * Credit:
 *  In-Depth Guide to Coroutine Cancellation & Exception Handling - Android Studio Tutorial
 *      https://www.youtube.com/watch?v=VWlwkqmTLHc&t=599s : by Phillipp Lackner.
 *      https://www.youtube.com/watch?v=CuWJBcOuNHI : by Phillipp Lackner.
 *      https://www.youtube.com/watch?v=e7tKQDJsTGs : by Dave leeds
 *
 *
 *
 */

suspend fun main() {
    logger = Logger.getLogger("Coroutine-01")
    logger.log("Main entry...")
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        logger.log("exceptionHandler@main :message: ${throwable.message}\nprintStackTrace: ${throwable.printStackTrace()}")
    }
    //if one coroutine fails insider this scope only that specific coroutine terminated not all because of SupervisorJob
    //without SupervisorJob all of them will be cancelled.
    var scope: CoroutineScope? =
        CoroutineScope(AppDispatcher.default + exceptionHandler + SupervisorJob())
    scope?.launch {
        //exceptionHandlingMain01()
        //exceptionHandlingMain02()
        val job1 = launch { establishDummyApi() }
        launch {
            delay(2000L)
            job1.cancel()
            logger.log("job1.cancel")
        }

    }.also {
        it?.invokeOnCompletion {
            scope = null
            logger.log("clean-up scope $scope")
        }
    }?.join()

    //coroutineScope { cancellationExceptionMain01() }
    //scope = CoroutineScope(Dispatchers.Default+exceptionHandler)

    /*scope?.launch {
        val job1 = launch {
            try {
                for (i in 1..10){
                    logger.info("i $i")
                    delay(1000L)
                }
            }catch (exception:Exception){
                logger.info("exception: ${exception.message}")
            }
        }
        launch {
            delay(2000L)
            job1.cancel()
            logger.info("job1 isCancelled ${job1.isCancelled}")
        }
    }?.also {
        it?.invokeOnCompletion {
            logger.log("invokeOnCompletion ${it?.message}")
        }
    }?.join()*/
    logger.log("Main exit...")
}

//Exception: will cause both of the coroutine to end
//CancellationException: will only cause that specific coroutine to terminate
suspend fun exceptionHandlingMain01() {
    coroutineScope {
        this.launch {
            launch {
                launch {
                    /*for (i in 1..10) {
                        logger.log("i $i")
                        delay(500L)
                        if (i == 5) throw CancellationException("CancellationException @5")//Exception("Exception @5")
                    }*/
                    task01()
                }

                launch {
                    /*for (j in 1..10) {
                        logger.log("j $j")
                        delay(500L)
                        //if (j == 5) throw Exception("Exception @5")
                    }*/
                    task02()
                }
            }
        }
    }
}


suspend fun task01(coroutineDispatcher: CoroutineDispatcher = AppDispatcher.default) {
    withContext(coroutineDispatcher) {
        for (i in 1..10) {
            logger.log("task01 $i")
            delay(500L)
            if (i == 5) throw /*CancellationException("CancellationException @5")*/Exception("Exception @5")
        }
    }
}

suspend fun task02(coroutineDispatcher: CoroutineDispatcher = AppDispatcher.default) {
    withContext(coroutineDispatcher) {
        for (i in 1..10) {
            logger.log("task02 $i")
            delay(500L)
        }
    }
}


suspend fun exceptionHandlingMain02() {
    val coroutineExceptionHandler1 = CoroutineExceptionHandler { coroutineContext, throwable ->
        logger.log("coroutineExceptionHandler1@${Thread.currentThread().name} :message: ${throwable.message}\nprintStackTrace: ${throwable.printStackTrace()}")
    }
    /*coroutineScope*/supervisorScope {
        launch/*(coroutineExceptionHandler1)*/ {
            task01()
        }
        launch {
            task02()
        }
    }
}


//Cancellation Exception:

suspend fun cancellationExceptionMain01() {
    val handler =
        CoroutineExceptionHandler { coroutineContext, throwable -> logger.log("CoroutineExceptionHandler@ $coroutineContext\nmessage: ${throwable.message}\nprintStackTrace ${throwable.printStackTrace()}") }
    val scope = CoroutineScope(AppDispatcher.default + handler + SupervisorJob())
    scope.launch {
        val job1 = launch {
            try {
                cancellationException01()
            } catch (exception: Exception) {
                exception.message?.let { logger.log(it) }
                //if(exception is CancellationException) throw exception
            }
        }
        launch {
            delay(2000L)
            job1.cancel()
            logger.log("called job1")
            //job1.join()
        }
    }.join()
}

suspend fun cancellationException01(dispatcher: CoroutineDispatcher = AppDispatcher.default) {
    withContext(dispatcher) {
        try {
            for (i in 1..100) {
                logger.log("i $i")
                delay(1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            //throw e
        }
    }
}


/**** coroutineCancellation: credit: https://www.youtube.com/watch?v=CuWJBcOuNHI
 * If coroutine is on canceled state, it won't executes the suspend functions
 * it'll skip suspend call and only executes non-suspending call.
 * to execute the suspend functions on the coroutine canceled state need to use the withContext(NonCancellable) block.
 */
suspend fun establishDummyApi(){
    try {
        getDummyApiCall()
    }catch (e:Exception){
        logger.info("establishDummyApi :"+e.message)
        if(e is CancellationException) throw e
        /*throw e*/
    }finally {
        logger.info("entered finally block")
        //clearDummyApiConnection() //
        withContext(NonCancellable){ //only use clean-up code, control will be lost.
            clearDummyApiConnection()
        }
    }
}

suspend fun getDummyApiCall(){
    withContext(AppDispatcher.default){
        while (true) {
            logger.info("getDummyApiCall.....")
            delay(1000L)
        }
    }
}

suspend fun clearDummyApiConnection(){
    withContext(AppDispatcher.default){
        logger.info("clearing DummyApi connection")
        delay(100L)
        logger.info("cleared DummyApi.....")
    }
}