Travel Policy & City Tier API Prompts ------------------------------------- 
MODULE: TRAVEL POLICY ------------------------------------- 
This module defines travel entitlements based on employee Grade and travel type. 
ENTITY: TRAVEL POLICY 
Fields: - PolicyID (Primary Key) - GradeID (FK → Grade table, e.g., G1–G6) - TravelType (Domestic / International) - FlightClass (Economy / Business / First) - HotelCategory (3Star / 4Star / 5Star) - PerDiemRate (Daily allowance) - LocalConveyanceLimit (Max local travel expense) - EffectiveDate (Policy start date) - Status (Active / Superseded) - CreatedDate (optional) - UpdatedDate (optional) 
REQUIREMENTS: 
1. CRUD APIs 
- Create, Update, Get, Deactivate policies 
2. Business Rules - One active policy per GradeID + TravelType - Supersede old policy on update - Validate positive values for allowances 
3. Validation - Grade must exist - Enumerations validated 
4. Filtering APIs - By GradeID, TravelType, Active status 
5. Advanced API - Fetch policy using GradeID + TravelType 
6. Database - FK on GradeID, indexed ------------------------------------- 
MODULE: CITY TIER ------------------------------------- 
This module defines location-based cost classifications. 
ENTITY: CITY TIER 
Fields: - CityID (Primary Key) - CityName - Country - Tier (Tier1 / Tier2 / Tier3 / International) - PerDiemRate - HotelCapPerNight - CreatedDate (optional) - UpdatedDate (optional) 
REQUIREMENTS: 
1. CRUD APIs - Create, Update, Get, Delete city tiers 
2. Business Rules - Unique city per country - Positive cost fields 
3. Validation - Tier enums - Required fields 
4. Filtering APIs - By Tier, Country 
5. Advanced API - Fetch city cost details 
6. Integration - Combine with TravelPolicy for final rules 
7. Database - Unique constraint on CityName + Country ------------------------------------- 
OUTPUT EXPECTED ------------------------------------- 
Provide: - Entity classes - DTOs - Repository - Service layer - Controller APIs - Validation - Sample JSON - SQL schema 
Ensure production-ready implementation. 
