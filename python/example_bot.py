"""
example_bot.py — Example Discord bot for Contrary Phone VPS.
BOT_TOKEN is automatically injected by the runtime — do NOT hardcode tokens.
"""
import discord
import asyncio

# BOT_TOKEN is injected by the app at runtime
intents = discord.Intents.default()
intents.message_content = True

client = discord.Client(intents=intents)

@client.event
async def on_ready():
    print(f"[Bot] Logged in as {client.user} (ID: {client.user.id})")
    print(f"[Bot] Connected to {len(client.guilds)} guild(s)")

@client.event
async def on_message(message: discord.Message):
    if message.author == client.user:
        return

    if message.content.startswith("!ping"):
        await message.channel.send("🏓 Pong! Bot is running on **Contrary Phone VPS**.")

    elif message.content.startswith("!status"):
        import platform
        await message.channel.send(
            f"✅ Running on Android via Contrary Phone VPS\n"
            f"Python: {platform.python_version()}"
        )

    elif message.content.startswith("!help"):
        embed = discord.Embed(
            title="Bot Commands",
            color=discord.Color.purple(),
        )
        embed.add_field(name="!ping", value="Check if bot is online", inline=False)
        embed.add_field(name="!status", value="Show runtime info", inline=False)
        embed.add_field(name="!help", value="Show this message", inline=False)
        await message.channel.send(embed=embed)


async def main():
    async with client:
        await client.start(BOT_TOKEN)

# Entry point — asyncio loop is managed by BotRunner
if __name__ == "__main__":
    asyncio.run(main())
