rootProject.name = "blue-bank"

// Application Services
include("app:account")
include("app:deposit")
include("app:loan")
include("app:card")
include("app:batch")

// Data Modules
include("data:account-data")
include("data:deposit-data")
include("data:loan-data")
include("data:card-data")

// Shared testing infrastructure
include("testing:test-support")
