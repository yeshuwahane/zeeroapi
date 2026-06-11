package com.alien.routes

import com.alien.models.BidRequest
import com.alien.models.Product
import com.alien.models.UploadProductRequest
import com.alien.plugins.ProductsTable
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.UUID

private var productsLastUpdated: Long = System.currentTimeMillis()

private fun notifyProductUpdate() {
    productsLastUpdated = System.currentTimeMillis()
}

fun Route.productRouting() {
    // Serve static uploads
    static("/uploads") {
        files("uploads")
    }

    route("/api/products") {
        get("/last-updated") {
            call.respond(HttpStatusCode.OK, mapOf("lastUpdated" to productsLastUpdated))
        }

        get {
            val products = transaction {
                ProductsTable.selectAll().map { row ->
                    Product(
                        id = row[ProductsTable.id],
                        title = row[ProductsTable.title],
                        description = row[ProductsTable.description],
                        price = row[ProductsTable.price],
                        imageUrl = row[ProductsTable.imageUrl],
                        supplierId = row[ProductsTable.supplierId],
                        isApproved = row[ProductsTable.isApproved],
                        currentHighestBid = row[ProductsTable.currentHighestBid],
                        highestBidderName = row[ProductsTable.highestBidderName],
                        auctionEndTimeMillis = row[ProductsTable.auctionEndTimeMillis]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, products)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing product ID.")
            val product = transaction {
                ProductsTable.select { ProductsTable.id eq id }.map { row ->
                    Product(
                        id = row[ProductsTable.id],
                        title = row[ProductsTable.title],
                        description = row[ProductsTable.description],
                        price = row[ProductsTable.price],
                        imageUrl = row[ProductsTable.imageUrl],
                        supplierId = row[ProductsTable.supplierId],
                        isApproved = row[ProductsTable.isApproved],
                        currentHighestBid = row[ProductsTable.currentHighestBid],
                        highestBidderName = row[ProductsTable.highestBidderName],
                        auctionEndTimeMillis = row[ProductsTable.auctionEndTimeMillis]
                    )
                }.firstOrNull()
            }

            if (product != null) {
                call.respond(HttpStatusCode.OK, product)
            } else {
                call.respond(HttpStatusCode.NotFound, "Product not found.")
            }
        }

        post {
            val request = call.receive<UploadProductRequest>()
            val newId = (transaction {
                ProductsTable.selectAll().mapNotNull { row -> row[ProductsTable.id].toIntOrNull() }.maxOrNull() ?: 0
            } + 1).toString()

            val endTime = if (request.isAuction) {
                System.currentTimeMillis() + (request.durationHours.toLong() * 3600L * 1000L)
            } else {
                0L
            }

            val newProduct = transaction {
                ProductsTable.insert {
                    it[id] = newId
                    it[title] = request.title
                    it[description] = request.description
                    it[price] = request.price
                    it[imageUrl] = request.category
                    it[supplierId] = request.supplierId
                    it[isApproved] = false
                    it[currentHighestBid] = 0.0
                    it[highestBidderName] = ""
                    it[auctionEndTimeMillis] = endTime
                }
                Product(
                    id = newId,
                    title = request.title,
                    description = request.description,
                    price = request.price,
                    imageUrl = request.category,
                    supplierId = request.supplierId,
                    isApproved = false,
                    currentHighestBid = 0.0,
                    highestBidderName = "",
                    auctionEndTimeMillis = endTime
                )
            }

            notifyProductUpdate()
            call.respond(HttpStatusCode.Created, newProduct)
        }

        post("/{id}/approve") {
            val requesterId = call.request.headers["X-User-Id"]
            if (requesterId == "adm_02") {
                return@post call.respond(HttpStatusCode.Forbidden, "Operations Manager does not have permission to approve listings.")
            }
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing product ID.")
            val success = transaction {
                ProductsTable.update({ ProductsTable.id eq id }) {
                    it[isApproved] = true
                } > 0
            }
            if (success) {
                notifyProductUpdate()
                call.respond(HttpStatusCode.OK, "Product approved.")
            } else {
                call.respond(HttpStatusCode.NotFound, "Product not found.")
            }
        }

        post("/{id}/reject") {
            val requesterId = call.request.headers["X-User-Id"]
            if (requesterId == "adm_02") {
                return@post call.respond(HttpStatusCode.Forbidden, "Operations Manager does not have permission to reject/delete listings.")
            }
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing product ID.")
            val success = transaction {
                ProductsTable.deleteWhere { ProductsTable.id eq id } > 0
            }
            if (success) {
                notifyProductUpdate()
                call.respond(HttpStatusCode.OK, "Product rejected/deleted.")
            } else {
                call.respond(HttpStatusCode.NotFound, "Product not found.")
            }
        }

        post("/{id}/bid") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing product ID.")
            val request = call.receive<BidRequest>()

            val product = transaction {
                ProductsTable.select { ProductsTable.id eq id }.map { row ->
                    Product(
                        id = row[ProductsTable.id],
                        title = row[ProductsTable.title],
                        description = row[ProductsTable.description],
                        price = row[ProductsTable.price],
                        imageUrl = row[ProductsTable.imageUrl],
                        supplierId = row[ProductsTable.supplierId],
                        isApproved = row[ProductsTable.isApproved],
                        currentHighestBid = row[ProductsTable.currentHighestBid],
                        highestBidderName = row[ProductsTable.highestBidderName],
                        auctionEndTimeMillis = row[ProductsTable.auctionEndTimeMillis]
                    )
                }.firstOrNull()
            } ?: return@post call.respond(HttpStatusCode.NotFound, "Product not found.")

            val minRequired = if (product.currentHighestBid > 0.0) product.currentHighestBid else product.price
            if (request.amount <= minRequired) {
                call.respond(HttpStatusCode.BadRequest, "Bid must be greater than $$minRequired")
                return@post
            }

            val success = transaction {
                ProductsTable.update({ ProductsTable.id eq id }) {
                    it[currentHighestBid] = request.amount
                    it[highestBidderName] = request.bidderName
                } > 0
            }

            if (success) {
                notifyProductUpdate()
                call.respond(HttpStatusCode.OK, "Bid placed successfully.")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to place bid.")
            }
        }

        post("/upload-image") {
            val multipart = call.receiveMultipart()
            var fileName = ""
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val fileBytes = part.streamProvider().readBytes()
                    val uploadDir = File("uploads")
                    if (!uploadDir.exists()) uploadDir.mkdirs()
                    val ext = part.originalFileName?.substringAfterLast('.', "png") ?: "png"
                    fileName = "${UUID.randomUUID()}.$ext"
                    File(uploadDir, fileName).writeBytes(fileBytes)
                }
                part.dispose()
            }
            if (fileName.isNotEmpty()) {
                call.respond(HttpStatusCode.OK, mapOf("url" to "/uploads/$fileName"))
            } else {
                call.respond(HttpStatusCode.BadRequest, "No image file uploaded.")
            }
        }
    }
}
