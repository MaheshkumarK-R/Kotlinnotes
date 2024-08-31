package com.mahikr.kotlinnotes.threading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Logger
import kotlin.concurrent.thread

/***** Best practices with Coroutine:
 *
 *  Main safe[Dispatchers]
 *  Consider of cancellation at each suspending function call[re-throwing-exceptions]
 *  If tasks are independent then delegate tasks to multiple coroutines[launch/async/withContext...]
 *  If task is larger then do it chunk by chunk and consider cancellation[ex reading huge file, read byte by bytes]
 *  Make sure of lifecycle/lifespan and consider of notifying the root scope on Exceptions[supervisorScope/coroutineScope].
 *  Make sure to release resources. make use of AutoCloseable[Generic], Closeable[IO]
 */

//Basic tasks
private fun basicRoutine(taskId: Int, delay: Long) {
    println("basicRoutine: entry taskId > $taskId")
    Thread.sleep(delay)
    println("basicRoutine: exit taskId > $taskId")
}
private fun basicThreadRoutine(taskId: Int, delay: Long) {
    thread(
        start = true,
        isDaemon = false,
        name = "Thread-$taskId",
        priority = Thread.NORM_PRIORITY
    ) {
        routine(taskId = taskId, delay = delay)
    }.join()
    //thread {  }.start()
}

private fun routine(taskId: Int, delay: Long) {
    println("routine: entry taskId > $taskId")
    Thread.sleep(delay)
    println("routine: exit taskId > $taskId")
}

/*****Coroutines
 * Coroutine doesn't block the calling thread and it shouldn't do it.
 * joinAll(<>,<>,...) //block the thread until tasks/coroutine inside it completes
 * coroutineScope{
 *      <>        //suspends the thread and only allow the thread/coroutine to executes tasks
 *      <>        //outside the block only if the tasks inside the block completes
 *     }          //inside block tasks will executes concurrently
 */

private suspend fun basicCoroutine(taskId: Int, pause: Long) {
    println("basicRoutine: entry taskId > $taskId")
    delay(pause)
    println("basicRoutine: exit taskId > $taskId")
}

//Main
/*
private fun main(){
    println("Main start...")
    basicRoutine(1, 250)
    basicRoutine(1, 500)
    println("Main end...")
}
*/

/*
private suspend fun main()=
    //runBlocking
    //coroutineScope {
    println("Main start...")
    //joinAll(
    //    launch { basicCoroutine(1,250) },
    //    launch { basicCoroutine(2,500) }
    //)

    coroutineScope {
        launch { basicCoroutine(1,250) }
        launch { basicCoroutine(2,500) }
    }
    println("Main end...")
}
*/

/*private fun main() {
    println("Main entry")
    thread {
        //val t1= thread{routine(1,500)}
        //val t2=
            //thread { routine(2,450) }
          //  thread(start = true, isDaemon = false, name = "thread-2", priority = Thread.NORM_PRIORITY){routine(2,450)}
        //t1.join()
        //t2.join()
        basicThreadRoutine(1, 250)
        basicThreadRoutine(2, 500)
    }.join()

    println("Main exit")
}*/

/*
private suspend fun main()= coroutineScope {
    println("entry main...")
    coroutineScope {
        println("before launch ${Thread.currentThread().name}")
        launch {
            println("Coroutine without  withContext ${Thread.currentThread().name}")
            delay(3000L)
            println("after delay ${Thread.currentThread().name}")
        }
        launch {
            withContext(Dispatchers.Default) {
                println("Default ${Thread.currentThread().name}")
                delay(2000L)
                println("after Default delay ${Thread.currentThread().name}")
            }
        }
    }
    println("exit main...")
}
*/


/*****Coroutine:
 *
 * Intro: Threads. Coroutine. https://docs.google.com/document/d/1inpbW76Lkfd-cRaYAvsfcJRPoDDd-9cXzkLaTQ3VPQs/edit
 *
 * Youtube: https://www.youtube.com/watch?v=Wpco6IK1hmY&t=2s
 *                //https://www.youtube.com/watch?v=2RdHD0tceL4
 *
 * Thread: Heavy handed DS managed by JVM & OS. to run the processor.
 * Shares a-lot of context. scheduling consumes a lot of performing computation.
 *
 *
 */

lateinit var logger: Logger
fun Logger.log(msg: String) = this.info("[${Thread.currentThread().name}] : $msg")

//Main
private suspend fun main() {
    logger = Logger.getLogger("Coroutine-01")
    logger.log("Main entry!!!")
    //bathTime()
    //sequentialMorningRoutine()
    //concurrentMorningRoutine()
    //plannedConcurrentMorningRoutine()
    //prepareCoffee()
    //workHardRoutine()
    //forgetFriendBirthdayRoutine()
    //workingNicelyWithResponse()
    //stayHydratedWhileWorking()
    asyncGreeting()
}

//tasks
suspend fun bathTime() {
    withContext(Dispatchers.Default) {
        logger.log("Going to bathroom...")
        delay(2000L)//suspend the coroutine
        logger.log("Bath done exiting!!!")
    }
}

suspend fun boilingWater() {
    logger.log("Boiling water")
    delay(1000L)
    logger.log("Water boiled")
}

suspend fun prepareBreakFast() {
    withContext(Dispatchers.Default) {
        logger.log("start preparing breakfast...")
        delay(2000L)//suspend the coroutine
        logger.log("Breakfast prepared!!!")
    }
}

suspend fun eatBreakFast() {
    withContext(Dispatchers.Default) {
        logger.log("start eating breakfast...")
        delay(2000L)//suspend the coroutine
        logger.log("ate breakfast!!!")
    }
}

suspend fun prepareJavaCoffee(): String {
    return withContext(Dispatchers.Default) {
        logger.log("Start preparing JavaCoffee")
        delay(500L)
        logger.log("Java coffee is prepared!!!")
        "JAVA COFFEE IS READY☕"
    }
}

suspend fun prepareCappuccino() = withContext(Dispatchers.Default) {
    logger.log("Start preparing Cappuccino")
    delay(500L)
    logger.log("Java coffee is Cappuccino!!!")
    "Cappuccino is ready☕"
}

//Structured concurrency...
/*****coroutineScope: Construct. wrapper over suspend function. start a context for a coroutine.
 * Everything inside the coroutineScope block will be isolated.
 * to finish scope, all the coroutine inside the block must complete
 */
suspend fun sequentialMorningRoutine() {

    coroutineScope {
        bathTime()
        //add codes, including suspend functions.
        //parallel code can be placed...
        //batch of parallel codes.
        //allow to share context, do the parenting feature{cancellation & etc}

    }
    coroutineScope {
        boilingWater()
    }
}

private suspend fun concurrentMorningRoutine() {
    logger.log("concurrentMorningRoutine entry")
    coroutineScope { //concurrent executions
        launch(Dispatchers.Default) { bathTime() }
        launch(Dispatchers.Default) { boilingWater() }
    }
    logger.log("concurrentMorningRoutine exit!!!")
}


/*****Plan coroutine task execution
 * 1. take bath & start boiling water
 * 2. after both tasks done. prepare breakfast and eat
 */
private suspend fun plannedConcurrentMorningRoutine() {/*//way: 1 //managing job manually
    coroutineScope {
        val boilingWaterJob:Job = launch { boilingWater() }
        val bathTimeJob:Job = launch { bathTime() }
        joinAll(boilingWaterJob, bathTimeJob)//block until both job done!!
        prepareBreakFast()
        eatBreakFast()
    }*/

    /* //way: 2 //nested job
    coroutineScope {
        val boilingWaterJob:Job = launch { boilingWater() }
        val bathTimeJob:Job = launch { bathTime() }
    }
    prepareBreakFast()
    eatBreakFast()*/
    //nested coroutineScope
    coroutineScope {
        coroutineScope {
            launch { boilingWater() }
            launch { bathTime() }
        }/*launch {*/
        prepareBreakFast()
        eatBreakFast()/*}*/
    }

}

private suspend fun prepareCoffee() {
    coroutineScope {
        val deferredJob1: Deferred<String> =
            async(start = CoroutineStart.LAZY) { prepareJavaCoffee() }
        val deferredJob2: Deferred<String> = async { prepareCappuccino() }
        logger.log(deferredJob2.await())
        launch {
            delay(10_000L)
            logger.log("after 10k delay ")
            logger.log(deferredJob1.await())
        }
    }
}


//1. Threading, pre-emitive scheduling, OS decides when thread finish executing and save context and resume thread exception and etc. expensive
//1. Co-operative scheduling, coroutine yield manually. ie. suspending point..
private suspend fun workingHard() {
    //withContext(Dispatchers.Default) {
    logger.log("workingHard...")
    while (true) {
        //doing heavy-computation task....
    }
    delay(3000L)
    logger.log("work done!!")
    // }
}

private suspend fun workingNicely() {
    //withContext(Dispatchers.Default) {
    logger.log("workingNicely...")
    while (true) {
        //doing heavy-computation task....
        delay(100L)
    }
    delay(3000L)
    logger.log("work done!!")
    // }
}


private suspend fun workingNicelyWithResponse() {
    //withContext(Dispatchers.Default) {
    logger.log("workingNicelyWithResponse...")

    coroutineScope {
        val workingNicelyJob = launch {
            Desk().use {//desk will be closed on coroutine complete...
                workingNicely()
            }
        }.also { job ->
            job.invokeOnCompletion {
                //can to clean-up manually @here
                logger.log("workingNicelyJob: invokeOnCompletion ${it?.message}")
                it?.printStackTrace()
            }
        }

        launch {
            delay(2000L)
            workingNicelyJob.cancel()
            workingNicelyJob.join()
            logger.log("Going out!!!")
        }

    }
}

private suspend fun takeABreak() {
    //  withContext(Dispatchers.Default) {
    logger.log("takeABreak <.|.|.>")
    delay(500L)
    logger.log("break done!!!")
    //  }
}


private suspend fun workHardRoutine() {
    val dispatcher = Dispatchers.Default.limitedParallelism(1)
    coroutineScope {
        launch(dispatcher) {
            //workingHard()
            workingNicely()
        }
        launch(dispatcher) { takeABreak() }
    }
}

//If there is no yielding point no cancellation will happen
private suspend fun forgetFriendBirthdayRoutine() {
    coroutineScope {
        val workingNicelyJob: Job = launch {
            workingNicely() //has yielding point
            //workingHard() //has no yielding point
        }
        launch {
            delay(2000L)
            logger.log("Oops I forgot my friend birthday")
            workingNicelyJob.cancel(CancellationException("Oops I forgot my friend birthday >> CancellationException"))//cancellation happens @first yielding point
            workingNicelyJob.join()
            logger.log("Going to buy present now!!")
        }
    }
}

//Cancellation will propagate upward.
private suspend fun drinkingWater() {
    while (true) {
        logger.log("Drinking water")
        delay(1000L)
        logger.log("Drunk water")

    }
}

//
private suspend fun stayHydratedWhileWorking() {
    coroutineScope {
        val workingJob = launch {
            launch { workingNicely() }
            launch { drinkingWater() }
        }
        launch {
            delay(1000L)
            workingJob.cancel()
            workingJob.join()
            logger.log("stayHydratedWhileWorking completed")
        }
    }
}

//CoroutineContext: DS coroutine to get started...
suspend fun asyncGreeting(){
    coroutineScope {
        val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            logger.log("CoroutineExceptionHandler ${throwable.message}")
        }
        launch (CoroutineName("GreetCoroutine")+/*Dispatchers.Default*/AppDispatcher.default+coroutineExceptionHandler){
            logger.log("[Parent] Hello everyone $this")
            launch {//inherited from immediate parent if not mentioned
                logger.log("[Child]  Hello everyone $this")
                launch (Dispatchers.IO){//inherited from immediate parent if not mentioned
                    logger.log("[nested Child]  Hello everyone $this")
                }
            }
            delay(200L)
            logger.log("[Parent]  again Hello everyone $this")
        }
    }
}


/***** Architectural pattern to inject Dispatcher instead of Hard coding the Dispatcher
 * allow to use the testDispatcher and inject specific Dispatcher in the Production code flow.
 */
sealed interface IAppDispatcher {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val customDispatcher: CoroutineDispatcher
}

data object AppDispatcher : IAppDispatcher {
    override val main: MainCoroutineDispatcher
        get() = Dispatchers.Main
    override val io: CoroutineDispatcher
        get() = Dispatchers.IO
    override val default: CoroutineDispatcher
        get() = Dispatchers.Default
    override val customDispatcher: CoroutineDispatcher
        get() = Executors.newFixedThreadPool(10).asCoroutineDispatcher()
}

//Resource clean-up.
//AutoCloseable : Generic Resource clean-up
//Closeable     : IO Resource clean-up
private class Desk : AutoCloseable {
    init {
        logger.log("Started working on desk....")
    }

    override fun close() {
        logger.log("close desk been cleaned!!")
    }
}
