package examples.chainActor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.annotation.tailrec

/**
  * An implementation of ChainActor (ref. "Actors In Scala") using akka v2.0 APIs
  */
object ChainActorTest extends App {
  val system = ActorSystem("mySystem")
  val numActors = 1000000
  val masterActor = system.actorOf(Props(new MasterActor(numActors)))
  println(s"Starting MasterActor with $numActors actors...")
  val start = System.currentTimeMillis
  masterActor ! 'Start
  val elapsed = System.currentTimeMillis - start
  println(s"MasterActor started.  Took $elapsed ms.")

  class MasterActor(val numActors: Int) extends Actor {
    val first = buildChain(numActors, None)

    @tailrec
    final def buildChain(size: Int, next: Option[ActorRef]): ActorRef = {
      val a = context.actorOf(Props(new ChainActor(numActors - size + 1, next)))
      if (size > 1) buildChain(size - 1, Some(a))
      else a
    }

    def receive = {
      case 'Start =>
        first ! 'Die
      case 'Ack =>
        val elapsed = System.currentTimeMillis - start
        println(s"All actors died.  Took $elapsed ms.")
        system.terminate
    }
  }

  class ChainActor(id: Int, next: Option[ActorRef]) extends Actor {
    var from: ActorRef = _

    def receive = {
      case 'Die =>
        from = sender
        if (next.isEmpty) {
          from ! 'Ack
          context.stop(self)
        } else {
          next.get ! 'Die
        }
      case 'Ack => {
        context.stop(self)
        from ! 'Ack
      }
    }
  }

}
