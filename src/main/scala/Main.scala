/*
  This file is part of Rimbot.

  Rimbot is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Rimbot is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with Rimbot.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.fgsquad.rimbot

import jline.console.ConsoleReader
import scala.util.Try
import scala.util.Success
import scala.util.Failure

object Botrun {

  def main(args: Array[String]) {
    val settingsfile = "settings.json"

    def getConfig: Try[Config] = Config.readConfig(settingsfile)

    var arglist = args.toList

    val nomask: Character = null
    val passmask = '*'

    val reader = new ConsoleReader()

    val config = getConfig match {
      case Success(cf) => Some(cf)
      case Failure(er) => {
        reader.println(s"faild to oppen settins file $settingsfile")
        None
      }
    }

    val colonyfile = "colony.json"

    val verbose = config.map(c => c.verbose).getOrElse(true)

    val (name, stream, auth) = {
      val no = config.flatMap(c => c.botname)
      val so = config.flatMap(c => c.channel)
      val ao = config.flatMap(c => c.token)

      (
        no.getOrElse(reader.readLine("bot login name> ", nomask)),
        so.getOrElse(reader.readLine("stream name> ", nomask)),
        ao.getOrElse(reader.readLine("oauth token> ", passmask))
      )
    }

    val fulltoken = "oauth:" + auth

    val host = "irc.twitch.tv"
    val port = 6667

    val channel = s"#$stream"

    val setup = new BotSetup(name)

    setup.bot.setMessageDelay(2000)

    setup.bot.setVerbose(verbose);

    setup.bot.connect(host, port, fulltoken);

    val colonypickler = persist.ColonyPickler.pickler

    val colony = colonypickler.unpickle(colonyfile).recover {
      case err => {
        setup.bot.log("*********Error loading colony file************")
        setup.bot.log(err.getMessage())
        Colony()
      }
    }.get

    val fg = new FGSquaredHandler(colony)

    setup.join(stream, fg.rcv);

    val exit = reader.readLine("press enter to exit")
    colonypickler.pickle(fg.colony, colonyfile).recover { case ex => reader.println(s"failed to write out colony: ${ex.getMessage()}") }

    setup.bot.disconnect()
    setup.bot.dispose()

  }
}