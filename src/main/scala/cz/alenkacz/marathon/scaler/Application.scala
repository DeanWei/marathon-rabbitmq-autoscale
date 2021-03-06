package cz.alenkacz.marathon.scaler

import com.typesafe.scalalogging.StrictLogging
import cz.alenkacz.marathon.scaler.rabbitmq.Client

import scala.util.{Failure, Success, Try}

trait Application {
  def name: String
  def vhost: String
  def queueName: String
  def maxMessagesCount: Int
  def maxInstancesCount: Option[Int]
  def minInstancesCount: Option[Int]
  def rmqServerName: String
  def upCount: Int
  def downCount: Int
}

object ApplicationFactory extends StrictLogging {
  def tryCreate(rabbitMqClient: Client,
                name: String,
                rmqServerName: String,
                vhost: String,
                queueName: String,
                maxMessagesCount: Int,
                maxInstancesCount: Option[Int] = None,
                minInstancesCount: Option[Int] = None,
                upCount: Option[Int] = None,
                downCount: Option[Int] = None): Try[Application] = {
    rabbitMqClient.queueExists(vhost, queueName) match {
      case Success(true) =>
        Success(
          ApplicationImpl(name,
                          rmqServerName,
                          vhost,
                          queueName,
                          maxMessagesCount,
                          maxInstancesCount,
                          minInstancesCount,
                          upCount.getOrElse(1),
                          downCount.getOrElse(1)))
      case Failure(e) =>
        logger.warn(
          s"Unable to verify that '$queueName' for application '$name' exists. Ignoring this application configuration.",
          e)
        Failure(e)
      case _ =>
        logger.warn(
          s"Queue '$queueName' for application '$name' does not exist. Ignoring this application configuration.")
        Failure(
          new Exception(
            s"Queue '$queueName' for application '$name' does not exist."))
    }
  }

  private case class ApplicationImpl(name: String,
                                     rmqServerName: String,
                                     vhost: String,
                                     queueName: String,
                                     maxMessagesCount: Int,
                                     maxInstancesCount: Option[Int] = None,
                                     minInstancesCount: Option[Int] = None,
                                     upCount: Int,
                                     downCount: Int)
      extends Application
}
