// MongoDB initialization script
// This script runs when the MongoDB container starts for the first time

// Switch to the recipe_generator database
db = db.getSiblingDB('recipe_generator');

// Create collections with validation
db.createCollection('users', {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["googleId", "email", "name", "roles", "createdAt"],
         properties: {
            googleId: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            email: {
               bsonType: "string",
               pattern: "^.+@.+\..+$",
               description: "must be a valid email address and is required"
            },
            name: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            picture: {
               bsonType: "string",
               description: "must be a string if provided"
            },
            roles: {
               bsonType: "array",
               items: {
                  bsonType: "string",
                  enum: ["USER", "ADMIN"]
               },
               description: "must be an array of valid roles"
            },
            createdAt: {
               bsonType: "date",
               description: "must be a date and is required"
            },
            lastLoginAt: {
               bsonType: "date",
               description: "must be a date if provided"
            }
         }
      }
   }
});

db.createCollection('recipes', {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["title", "ingredients", "steps", "timeMinutes", "difficulty", "source"],
         properties: {
            title: {
               bsonType: "string",
               minLength: 1,
               description: "must be a non-empty string and is required"
            },
            ingredients: {
               bsonType: "array",
               minItems: 1,
               items: {
                  bsonType: "object",
                  required: ["name"],
                  properties: {
                     name: { bsonType: "string" },
                     quantity: { bsonType: "string" },
                     unit: { bsonType: "string" }
                  }
               },
               description: "must be an array with at least one ingredient"
            },
            steps: {
               bsonType: "array",
               minItems: 1,
               items: { bsonType: "string" },
               description: "must be an array with at least one step"
            },
            timeMinutes: {
               bsonType: "int",
               minimum: 1,
               description: "must be a positive integer"
            },
            difficulty: {
               bsonType: "string",
               enum: ["EASY", "MEDIUM", "HARD"],
               description: "must be a valid difficulty level"
            },
            cuisine: {
               bsonType: "string",
               description: "must be a string if provided"
            },
            dietTags: {
               bsonType: "array",
               items: { bsonType: "string" },
               description: "must be an array of strings if provided"
            },
            source: {
               bsonType: "string",
               enum: ["DB", "LLM", "FALLBACK"],
               description: "must be a valid source type"
            },
            createdBy: {
               bsonType: "string",
               description: "must be a string if provided"
            },
            createdAt: {
               bsonType: "date",
               description: "must be a date if provided"
            }
         }
      }
   }
});

db.createCollection('ratings');
db.createCollection('favorites');
db.createCollection('logs');

// Create indexes for better performance
db.users.createIndex({ "googleId": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });

db.recipes.createIndex({ "title": "text", "ingredients.name": "text" });
db.recipes.createIndex({ "cuisine": 1 });
db.recipes.createIndex({ "difficulty": 1 });
db.recipes.createIndex({ "timeMinutes": 1 });
db.recipes.createIndex({ "dietTags": 1 });
db.recipes.createIndex({ "source": 1 });
db.recipes.createIndex({ "createdBy": 1 });
db.recipes.createIndex({ "createdAt": -1 });

db.ratings.createIndex({ "userId": 1, "recipeId": 1 }, { unique: true });
db.ratings.createIndex({ "recipeId": 1 });
db.ratings.createIndex({ "createdAt": -1 });

db.favorites.createIndex({ "userId": 1, "recipeId": 1 }, { unique: true });
db.favorites.createIndex({ "userId": 1 });
db.favorites.createIndex({ "recipeId": 1 });
db.favorites.createIndex({ "createdAt": -1 });

db.logs.createIndex({ "timestamp": -1 });
db.logs.createIndex({ "event": 1 });
db.logs.createIndex({ "userId": 1 });
db.logs.createIndex({ "level": 1 });

print('Recipe Generator database initialized successfully');
print('Collections created: users, recipes, ratings, favorites, logs');
print('Indexes created for optimal query performance');



