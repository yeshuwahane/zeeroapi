package com.alien.plugins

import com.alien.models.UserRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

object UsersTable : Table("users") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 100)
    val role = varchar("role", 20)

    override val primaryKey = PrimaryKey(id)
}

object ProductsTable : Table("products") {
    val id = varchar("id", 50)
    val title = varchar("title", 100)
    val description = text("description")
    val price = double("price")
    val imageUrl = varchar("image_url", 255)
    val supplierId = varchar("supplier_id", 50)
    val isApproved = bool("is_approved")
    val currentHighestBid = double("current_highest_bid")
    val highestBidderName = varchar("highest_bidder_name", 100)
    val auctionEndTimeMillis = long("auction_end_time_millis")

    override val primaryKey = PrimaryKey(id)
}

object ImagesTable : Table("images") {
    val id = varchar("id", 50)
    val content = text("content")

    override val primaryKey = PrimaryKey(id)
}

fun generatePlaceholderImage(fileName: String, text: String, color1: Color, color2: Color) {
    val dir = File("uploads")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, fileName)
    if (file.exists()) return

    val width = 600
    val height = 400
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g2d = bufferedImage.createGraphics()

    // Draw gradient background
    val gp = GradientPaint(0f, 0f, color1, width.toFloat(), height.toFloat(), color2)
    g2d.paint = gp
    g2d.fillRect(0, 0, width, height)

    // Draw text overlay
    g2d.color = Color.WHITE
    g2d.font = Font("Arial", Font.BOLD, 36)
    val metrics = g2d.fontMetrics
    val x = (width - metrics.stringWidth(text)) / 2
    val y = ((height - metrics.height) / 2) + metrics.ascent
    g2d.drawString(text, x, y)
    g2d.dispose()

    ImageIO.write(bufferedImage, "png", file)
}

fun convertDatabaseUrlToJdbc(url: String): String {
    if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
        val uri = URI(url)
        val port = if (uri.port == -1) "" else ":${uri.port}"
        val query = buildList {
            if (!uri.rawQuery.isNullOrBlank()) add(uri.rawQuery)

            val userInfo = uri.rawUserInfo?.split(":", limit = 2)
            if (userInfo?.size == 2) {
                add("user=${userInfo[0]}")
                add("password=${userInfo[1]}")
            }
        }.joinToString("&")
        val querySuffix = if (query.isBlank()) "" else "?$query"

        return "jdbc:postgresql://${uri.host}$port${uri.rawPath}$querySuffix"
    }
    return url
}

fun configureDatabase() {
    val rawDbUrl = System.getenv("JDBC_DATABASE_URL") ?: System.getenv("DATABASE_URL")
    val database = if (!rawDbUrl.isNullOrBlank()) {
        val dbUrl = convertDatabaseUrlToJdbc(rawDbUrl)
        val driver = if (dbUrl.contains("postgresql")) "org.postgresql.Driver" else "org.sqlite.JDBC"
        Database.connect(
            url = dbUrl,
            driver = driver
        )
    } else {
        val dbFile = File("zeerostock.db")
        Database.connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC"
        )
    }

    generatePlaceholderImage("iphone15.png", "iPhone 15 Pro Max", Color(0xFF, 0x3B, 0x30), Color(0xFF, 0x95, 0x00))
    generatePlaceholderImage("macbook.png", "MacBook Pro M3", Color(0x5A, 0x2A, 0x8C), Color(0x30, 0x75, 0xDC))
    generatePlaceholderImage("headphones.png", "Sony WH-1000XM5", Color(0x00, 0xC6, 0xFF), Color(0x00, 0x72, 0xFF))
    generatePlaceholderImage("jordan1.png", "Air Jordan 1 Chicago", Color(0xF9, 0xD4, 0x23), Color(0xFF, 0x4E, 0x50))
    generatePlaceholderImage("jacket.png", "Vintage Leather Jacket", Color(0x4E, 0x54, 0xC8), Color(0x8F, 0x94, 0xFB))
    generatePlaceholderImage("keyboard.png", "Gaming Keyboard", Color(0x11, 0x99, 0x8E), Color(0x38, 0xEF, 0x7D))

    transaction(database) {
        SchemaUtils.create(UsersTable, ProductsTable, ImagesTable)

        // Seed users
        if (UsersTable.selectAll().count() == 0L) {
            UsersTable.insert {
                it[id] = "usr_01"
                it[name] = "Alice Smith"
                it[email] = "alice@customer.com"
                it[password] = "alice123"
                it[role] = UserRole.CUSTOMER.name
            }
            UsersTable.insert {
                it[id] = "usr_02"
                it[name] = "Bob Jones"
                it[email] = "bob@customer.com"
                it[password] = "bob123"
                it[role] = UserRole.CUSTOMER.name
            }
            UsersTable.insert {
                it[id] = "usr_03"
                it[name] = "Charlie Brown"
                it[email] = "charlie@customer.com"
                it[password] = "charlie123"
                it[role] = UserRole.CUSTOMER.name
            }
            UsersTable.insert {
                it[id] = "sup_01"
                it[name] = "Global Tech Supplies"
                it[email] = "info@globaltech.com"
                it[password] = "global123"
                it[role] = UserRole.SUPPLIER.name
            }
            UsersTable.insert {
                it[id] = "sup_02"
                it[name] = "Apex Electronics"
                it[email] = "sales@apexelectronics.com"
                it[password] = "apex123"
                it[role] = UserRole.SUPPLIER.name
            }
            UsersTable.insert {
                it[id] = "sup_03"
                it[name] = "Retro Thrift Co."
                it[email] = "retrothrift@gmail.com"
                it[password] = "retro123"
                it[role] = UserRole.SUPPLIER.name
            }
            UsersTable.insert {
                it[id] = "adm_01"
                it[name] = "Chief Admin"
                it[email] = "admin@zeerostock.com"
                it[password] = "admin123"
                it[role] = UserRole.ADMIN.name
            }
            UsersTable.insert {
                it[id] = "adm_02"
                it[name] = "Operations Manager"
                it[email] = "manager@zeerostock.com"
                it[password] = "manager123"
                it[role] = UserRole.ADMIN.name
            }
        }

        // Migrate product imageUrls if they have the old non-url
        val hasOldImageUrl = ProductsTable.selectAll().any { row ->
            val url = row[ProductsTable.imageUrl]
            url == "electronics" || url == "audio" || url == "fashion"
        }
        if (hasOldImageUrl) {
            ProductsTable.deleteAll()
        }

        // Seed products
        if (ProductsTable.selectAll().count() == 0L) {
            val now = System.currentTimeMillis()
            ProductsTable.insert {
                it[id] = "1"
                it[title] = "iPhone 15 Pro Max (256GB)"
                it[description] = "Titanium design, A17 Pro chip, customizable Action button, and the most powerful iPhone camera system ever. Excellent condition, barely used."
                it[price] = 999.00
                it[imageUrl] = "/uploads/iphone15.png,electronics,audio"
                it[supplierId] = "sup_01"
                it[isApproved] = true
                it[currentHighestBid] = 1050.00
                it[highestBidderName] = "Alice Smith"
                it[auctionEndTimeMillis] = now + 3600000L * 2L
            }
            ProductsTable.insert {
                it[id] = "2"
                it[title] = "MacBook Pro M3 Max"
                it[description] = "16-inch liquid retina XDR display, 36GB unified memory, 1TB SSD. The ultimate powerhouse for developers and creators alike. Sealed in box."
                it[price] = 2499.00
                it[imageUrl] = "/uploads/macbook.png,electronics,audio"
                it[supplierId] = "sup_02"
                it[isApproved] = true
                it[currentHighestBid] = 2600.00
                it[highestBidderName] = "Bob Jones"
                it[auctionEndTimeMillis] = now + 3600000L * 5L
            }
            ProductsTable.insert {
                it[id] = "3"
                it[title] = "Sony WH-1000XM5 Headphones"
                it[description] = "Industry-leading noise canceling wireless headphones with auto noise-canceling optimizer, crystal clear hands-free calling, and Alexa voice control."
                it[price] = 348.00
                it[imageUrl] = "/uploads/headphones.png,audio"
                it[supplierId] = "sup_01"
                it[isApproved] = true
                it[currentHighestBid] = 0.0
                it[highestBidderName] = ""
                it[auctionEndTimeMillis] = 0L
            }
            ProductsTable.insert {
                it[id] = "4"
                it[title] = "Air Jordan 1 Retro High OG"
                it[description] = "Classic Chicago colorway, premium leather upper, comfortable air-sole cushioning. Perfect collector's item in size 10."
                it[price] = 180.00
                it[imageUrl] = "/uploads/jordan1.png,fashion"
                it[supplierId] = "sup_03"
                it[isApproved] = true
                it[currentHighestBid] = 210.00
                it[highestBidderName] = "Charlie Brown"
                it[auctionEndTimeMillis] = now + 3600000L * 12L
            }
            ProductsTable.insert {
                it[id] = "5"
                it[title] = "Vintage Leather Jacket"
                it[description] = "Genuine brown leather jacket, distressed style from the 90s. Heavyweight material, perfect for autumn and winter."
                it[price] = 120.00
                it[imageUrl] = "/uploads/jacket.png,fashion"
                it[supplierId] = "sup_03"
                it[isApproved] = false
                it[currentHighestBid] = 0.0
                it[highestBidderName] = ""
                it[auctionEndTimeMillis] = 0L
            }
            ProductsTable.insert {
                it[id] = "6"
                it[title] = "Mechanical Gaming Keyboard"
                it[description] = "Custom hot-swappable mechanical keyboard, linear yellow switches, PBT keycaps, RGB backlighting. Brand new construction."
                it[price] = 85.00
                it[imageUrl] = "/uploads/keyboard.png,electronics"
                it[supplierId] = "sup_02"
                it[isApproved] = false
                it[currentHighestBid] = 0.0
                it[highestBidderName] = ""
                it[auctionEndTimeMillis] = now + 3600000L * 24L
            }
        }
    }
}
