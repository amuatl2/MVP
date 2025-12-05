package com.example.mvp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object CreateAccount : Screen("create_account")
    object Dashboard : Screen("dashboard")
    object CreateTicket : Screen("create_ticket")
    object TicketDetail : Screen("ticket_detail/{ticketId}") {
        fun createRoute(ticketId: String) = "ticket_detail/$ticketId"
    }
    object Marketplace : Screen("marketplace/{ticketId}") {
        fun createRoute(ticketId: String? = null) = if (ticketId != null) "marketplace/$ticketId" else "marketplace/null"
    }
    object ContractorProfile : Screen("contractor_profile/{contractorId}") {
        fun createRoute(contractorId: String) = "contractor_profile/$contractorId"
    }
    object ContractorDashboard : Screen("contractor_dashboard")
    object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }
    object JobDetailTicket : Screen("job_detail_ticket/{ticketId}") {
        fun createRoute(ticketId: String) = "job_detail_ticket/$ticketId"
    }
    object Schedule : Screen("schedule/{ticketId}") {
        fun createRoute(ticketId: String? = null) = if (ticketId != null) "schedule/$ticketId" else "schedule/null"
    }
    object Rating : Screen("rating/{jobId}") {
        fun createRoute(jobId: String) = "rating/$jobId"
    }
    object JobCompletion : Screen("job_completion/{jobId}") {
        fun createRoute(jobId: String) = "job_completion/$jobId"
    }
    object ContractorScheduleJob : Screen("contractor_schedule_job/{jobId}") {
        fun createRoute(jobId: String) = "contractor_schedule_job/$jobId"
    }
    object TenantReview : Screen("tenant_review")
    object ReviewDetail : Screen("review_detail/{ticketId}") {
        fun createRoute(ticketId: String) = "review_detail/$ticketId"
    }
    object History : Screen("history")
    object Chat : Screen("chat")
    object AIDiagnosis : Screen("ai_diagnosis")
    object TenantLandlord : Screen("tenant_landlord")
    object LandlordTenants : Screen("landlord_tenants/{tenantEmail}") {
        fun createRoute(tenantEmail: String? = null) = 
            if (tenantEmail != null) "landlord_tenants/$tenantEmail" else "landlord_tenants/null"
    }
    object ContractorLandlordChat : Screen("contractor_landlord_chat/{ticketId}/{contractorId}") {
        fun createRoute(ticketId: String? = null, contractorId: String? = null) = 
            "contractor_landlord_chat/${ticketId ?: "general"}/${contractorId ?: ""}"
    }
    object ContractorLandlordConversation : Screen("contractor_landlord_conversation/{ticketId}/{landlordEmail}") {
        fun createRoute(ticketId: String, landlordEmail: String? = null) = 
            "contractor_landlord_conversation/$ticketId/${landlordEmail ?: ""}"
    }
    object ContractorService : Screen("contractor_service")
    object AssignContractor : Screen("assign_contractor/{ticketId}") {
        fun createRoute(ticketId: String) = "assign_contractor/$ticketId"
    }
    object LandlordTicketDetail : Screen("landlord_ticket_detail/{ticketId}") {
        fun createRoute(ticketId: String) = "landlord_ticket_detail/$ticketId"
    }
    object ContractorMessages : Screen("contractor_messages")
    object LandlordMessages : Screen("landlord_messages") {
        fun createRoute(contractorEmail: String? = null) = 
            if (contractorEmail != null && contractorEmail.isNotEmpty()) {
                "landlord_messages/$contractorEmail"
            } else {
                "landlord_messages"
            }
    }
    
    object LandlordMessagesWithContractor : Screen("landlord_messages/{contractorEmail}") {
        fun createRoute(contractorEmail: String) = "landlord_messages/$contractorEmail"
    }
    object TenantLandlordConversation : Screen("tenant_landlord_conversation/{tenantEmail}") {
        fun createRoute(tenantEmail: String? = null) = 
            if (tenantEmail != null) "tenant_landlord_conversation/$tenantEmail" else "tenant_landlord_conversation/general"
    }
    object TenantDetail : Screen("tenant_detail/{tenantEmail}") {
        fun createRoute(tenantEmail: String) = "tenant_detail/$tenantEmail"
    }
    object TenantMessages : Screen("tenant_messages")
    object TenantContractorConversation : Screen("tenant_contractor_conversation/{contractorEmail}") {
        fun createRoute(contractorEmail: String) = "tenant_contractor_conversation/$contractorEmail"
    }
    object ContractorTenantConversation : Screen("contractor_tenant_conversation/{tenantEmail}") {
        fun createRoute(tenantEmail: String) = "contractor_tenant_conversation/$tenantEmail"
    }
}

