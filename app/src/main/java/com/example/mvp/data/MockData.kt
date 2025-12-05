package com.example.mvp.data

object MockData {
    val mockTickets = listOf(
        Ticket(
            id = "1",
            title = "Leaking Kitchen Faucet",
            description = "The kitchen faucet has been dripping constantly for 2 days.",
            category = "Plumbing",
            status = TicketStatus.SUBMITTED,
            submittedBy = "tenant@home.com",
            submittedByRole = UserRole.TENANT,
            assignedTo = null,
            aiDiagnosis = "Plumbing - Likely requires washer replacement",
            createdAt = "2024-11-01T11:02:17Z",
            ticketNumber = "1",
            priority = "High"
        ),
        Ticket(
            id = "2",
            title = "Broken Light Switch",
            description = "The light switch in the hallway is not working.",
            category = "Electrical",
            status = TicketStatus.SUBMITTED,
            submittedBy = "tenant@example.com",
            submittedByRole = UserRole.TENANT,
            aiDiagnosis = "Electrical - Switch Replacement",
            createdAt = "2024-01-16T14:30:00Z",
            priority = "Medium"
        )
    )

    val mockContractors = listOf(
        Contractor(
            id = "contractor1",
            name = "John Smith",
            company = "ABC Plumbing",
            specialization = listOf("Plumbing", "HVAC"),
            rating = 4.8f,
            distance = 2.5f,
            preferred = true,
            completedJobs = 45
        ),
        Contractor(
            id = "contractor2",
            name = "Sarah Johnson",
            company = "Electric Solutions",
            specialization = listOf("Electrical"),
            rating = 4.6f,
            distance = 5.2f,
            preferred = false,
            completedJobs = 32
        ),
        Contractor(
            id = "contractor3",
            name = "Mike Davis",
            company = "All-in-One Maintenance",
            specialization = listOf("Plumbing", "Electrical", "HVAC"),
            rating = 4.9f,
            distance = 1.8f,
            preferred = true,
            completedJobs = 78
        )
    )

    val mockJobs = listOf(
        Job(
            id = "job1",
            ticketId = "1",
            contractorId = "contractor1",
            propertyAddress = "123 Main St, Apt 4B",
            issueType = "Plumbing",
            date = "2024-01-15",
            status = "assigned"
        )
    )
}

