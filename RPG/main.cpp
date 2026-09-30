#include <iostream>
#include <fstream>
#include <filesystem>
#include <algorithm>
#include <cctype>
#include <array>
#include <limits>
#include <random>
#include <string>
#include <vector>

struct Location {
    std::string name;
    std::string description;
};

struct Equipment {
    std::string helmet;
    std::string chestplate;
    std::string bracers;
    std::string leggings;
    std::string boots;
    std::string necklace;
    std::array<std::string, 2> bracelets{};
    std::array<std::string, 4> rings{};
};

struct Attributes {
    int strength;
    int agility;
    int constitution;
    int spirit;
};

struct Enemy {
    std::string name;
    int level;
    int health;
    int maxHealth;
    int armor;
    Attributes attributes;
};

struct Character {
    std::string name;
    std::string className;
    int health;
    int level;
    std::vector<std::string> inventory;
    Equipment equipment;
    Attributes attributes{};
    int experience = 0;
    int freeStatPoints = 0;
};

namespace fs = std::filesystem;
const fs::path saveDirectory = "characters";

Attributes getCharacterAttributes(const Character& character);
int getCharacterMaxHealth(const Character& character);
int getExperienceToNextLevel(int level);
int getEquipmentArmor(const Equipment& equipment);

void clearInput() {
    std::cin.clear();
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
}

Attributes generateAttributes(const std::string& className, bool randomize) {
    if (!randomize) {
        if (className == "Knight") return {5, 5, 8, 5};
        if (className == "Hunter") return {5, 8, 5, 5};
        if (className == "Mage") return {5, 5, 5, 8};
        if (className == "Swordsman") return {8, 5, 5, 5};
        return {5, 5, 5, 5};
    }

    static std::mt19937 generator(std::random_device{}());
    std::uniform_int_distribution<int> normalStat(1, 10);
    std::uniform_int_distribution<int> preferredStat(6, 10);
    Attributes attributes{
        normalStat(generator),
        normalStat(generator),
        normalStat(generator),
        normalStat(generator)
    };

    if (className == "Knight") attributes.constitution = preferredStat(generator);
    if (className == "Hunter") attributes.agility = preferredStat(generator);
    if (className == "Mage") attributes.spirit = preferredStat(generator);
    if (className == "Swordsman") attributes.strength = preferredStat(generator);
    return attributes;
}

std::string safeFileName(const std::string& name) {
    std::string result;

    for (const char character : name) {
        if (std::isalnum(static_cast<unsigned char>(character))) {
            result += character;
        } else if (character == ' ' || character == '_' || character == '-') {
            result += '_';
        }
    }

    return result.empty() ? "unnamed" : result;
}

bool characterNameTaken(const std::string& name) {
    std::error_code error;
    return fs::exists(saveDirectory / (safeFileName(name) + ".txt"), error) && !error;
}

bool saveCharacter(const Character& character) {
    std::error_code error;
    fs::create_directories(saveDirectory, error);
    if (error) {
        return false;
    }

    const fs::path savePath = saveDirectory / (safeFileName(character.name) + ".txt");
    std::ofstream saveFile(savePath);

    if (!saveFile) {
        return false;
    }

    saveFile << character.name << '\n';
    saveFile << character.className << '\n';
    saveFile << character.health << '\n';
    saveFile << character.level << '\n';
    saveFile << character.inventory.size() << '\n';
    for (const std::string& item : character.inventory) {
        saveFile << item << '\n';
    }
    saveFile << character.equipment.helmet << '\n';
    saveFile << character.equipment.chestplate << '\n';
    saveFile << character.equipment.bracers << '\n';
    saveFile << character.equipment.leggings << '\n';
    saveFile << character.equipment.boots << '\n';
    saveFile << character.equipment.necklace << '\n';
    for (const std::string& bracelet : character.equipment.bracelets) {
        saveFile << bracelet << '\n';
    }
    for (const std::string& ring : character.equipment.rings) {
        saveFile << ring << '\n';
    }
    saveFile << character.attributes.strength << ' '
             << character.attributes.agility << ' '
             << character.attributes.constitution << ' '
             << character.attributes.spirit << '\n';
    saveFile << character.experience << '\n';
    saveFile << character.freeStatPoints << '\n';
    return true;
}

void giveStarterEquipment(Character& character) {
    if (character.equipment.chestplate.empty()) {
        character.equipment.chestplate = "Starter Cloth Chestplate";
    }
    if (character.equipment.leggings.empty()) {
        character.equipment.leggings = "Starter Cloth Pants";
    }
    if (character.equipment.boots.empty()) {
        character.equipment.boots = "Starter Cloth Boots";
    }

    auto removeEquippedDuplicates = [&character](const std::string& equippedItem) {
        auto item = character.inventory.begin();
        while (item != character.inventory.end()) {
            if (*item == equippedItem) {
                item = character.inventory.erase(item);
            } else {
                ++item;
            }
        }
    };

    if (character.equipment.chestplate == "Starter Cloth Chestplate") {
        removeEquippedDuplicates("Starter Cloth Chestplate");
    }
    if (character.equipment.leggings == "Starter Cloth Pants") {
        removeEquippedDuplicates("Starter Cloth Pants");
    }
    if (character.equipment.boots == "Starter Cloth Boots") {
        removeEquippedDuplicates("Starter Cloth Boots");
    }
}

bool loadCharacterFromFile(const fs::path& savePath, Character& character) {
    std::ifstream saveFile(savePath);

    if (!saveFile) {
        return false;
    }

    if (!(
        std::getline(saveFile, character.name) &&
        std::getline(saveFile, character.className) &&
        (saveFile >> character.health) &&
        (saveFile >> character.level)
    )) {
        return false;
    }

    character.inventory.clear();
    character.equipment = {};

    // Older save files do not have an inventory count. They remain valid
    // and simply load with an empty inventory.
    std::size_t inventoryCount;
    if (saveFile >> inventoryCount) {
        std::string item;
        std::getline(saveFile, item);

        for (std::size_t i = 0; i < inventoryCount; ++i) {
            if (!std::getline(saveFile, item)) {
                return false;
            }
            character.inventory.push_back(item);
        }
    }

    // Equipment was added after the original inventory format. Older saves
    // simply keep the default empty equipment slots.
    std::string equipmentLine;
    if (std::getline(saveFile, equipmentLine)) {
        character.equipment.helmet = equipmentLine;
        if (!std::getline(saveFile, character.equipment.chestplate) ||
            !std::getline(saveFile, character.equipment.bracers) ||
            !std::getline(saveFile, character.equipment.leggings) ||
            !std::getline(saveFile, character.equipment.boots) ||
            !std::getline(saveFile, character.equipment.necklace)) {
            return false;
        }

        for (std::string& bracelet : character.equipment.bracelets) {
            if (!std::getline(saveFile, bracelet)) {
                return false;
            }
        }
        for (std::string& ring : character.equipment.rings) {
            if (!std::getline(saveFile, ring)) {
                return false;
            }
        }
    }

    character.attributes = generateAttributes(character.className, false);
    saveFile.clear();
    Attributes savedAttributes;
    if (saveFile >> savedAttributes.strength >> savedAttributes.agility
        >> savedAttributes.constitution >> savedAttributes.spirit) {
        character.attributes = savedAttributes;
    }

    character.experience = 0;
    saveFile.clear();
    saveFile >> character.experience;
    character.freeStatPoints = 0;
    saveFile.clear();
    saveFile >> character.freeStatPoints;

    giveStarterEquipment(character);
    character.health = std::min(character.health, getCharacterMaxHealth(character));
    if (character.health <= 0) {
        character.health = getCharacterMaxHealth(character);
    }
    saveCharacter(character);
    return true;
}

std::vector<fs::path> getSaveFiles() {
    std::vector<fs::path> saveFiles;
    std::error_code error;
    fs::create_directories(saveDirectory, error);

    if (error) {
        return saveFiles;
    }

    for (const fs::directory_entry& entry : fs::directory_iterator(saveDirectory, error)) {
        if (error) {
            return {};
        }

        if (entry.is_regular_file() && entry.path().extension() == ".txt") {
            saveFiles.push_back(entry.path());
        }
    }

    return saveFiles;
}

bool loadCharacter(Character& character) {
    const std::vector<fs::path> saveFiles = getSaveFiles();

    if (saveFiles.empty()) {
        return false;
    }

    std::cout << "\nSaved characters:\n";
    for (std::size_t i = 0; i < saveFiles.size(); ++i) {
        std::cout << i + 1 << ". " << saveFiles[i].stem().string() << '\n';
    }
    std::cout << "0. Go back\n";

    int saveChoice;
    while (true) {
        std::cout << "Choose a character: ";

        if (std::cin >> saveChoice && saveChoice >= 0 &&
            saveChoice <= static_cast<int>(saveFiles.size())) {
            break;
        }

        std::cout << "Please choose a valid character.\n";
        clearInput();
    }

    if (saveChoice == 0) {
        return false;
    }

    return loadCharacterFromFile(saveFiles[saveChoice - 1], character);
}

void showEquipped(const Equipment& equipment);
void deleteCharacter() {
    const std::vector<fs::path> saveFiles = getSaveFiles();

    if (saveFiles.empty()) {
        std::cout << "No saved characters were found.\n";
        return;
    }

    std::cout << "\nSaved characters:\n";
    for (std::size_t i = 0; i < saveFiles.size(); ++i) {
        std::cout << i + 1 << ". " << saveFiles[i].stem().string() << '\n';
    }
    std::cout << "0. Go back\n";

    int saveChoice;
    while (true) {
        std::cout << "Choose a character to delete: ";

        if (std::cin >> saveChoice && saveChoice >= 0 &&
            saveChoice <= static_cast<int>(saveFiles.size())) {
            break;
        }

        std::cout << "Please choose a valid character.\n";
        clearInput();
    }

    if (saveChoice == 0) {
        return;
    }

    const fs::path& savePath = saveFiles[saveChoice - 1];
    std::cout << "Delete " << savePath.stem().string() << "? (y/n): ";

    char confirmation;
    std::cin >> confirmation;
    if (confirmation != 'y' && confirmation != 'Y') {
        std::cout << "Character was not deleted.\n";
        return;
    }

    std::error_code error;
    if (fs::remove(savePath, error) && !error) {
        std::cout << "Character deleted.\n";
    } else {
        std::cout << "The character could not be deleted.\n";
    }
}

bool createCharacter(Character& character) {
    const std::string classes[] = {"Knight", "Mage", "Hunter", "Preist", "Swordsman"};
    const std::string preferences[] = {"Constitution", "Spirit", "Agility", "Spirit", "Strength"};
    constexpr int classCount = sizeof(classes) / sizeof(classes[0]);

    std::string name;
    while (true) {
        std::cout << "\nEnter a name for your character (or 0 to go back): ";
        std::cin >> std::ws;
        std::getline(std::cin, name);

        if (name == "0") {
            std::cout << "Returning to main menu.\n";
            return false;
        }

        if (name.empty()) {
            std::cout << "Character name cannot be empty.\n";
            continue;
        }

        if (characterNameTaken(name)) {
            std::cout << "That character name is already in use. Choose a different name.\n";
            continue;
        }

        break;
    }

    std::cout << "\nChoose a class:\n";
    for (int i = 0; i < classCount; ++i) {
        std::cout << i + 1 << ". " << classes[i]
                  << " (favors " << preferences[i] << ")\n";
    }

    int classChoice;
    while (true) {
        std::cout << "Enter your choice: ";

        if (std::cin >> classChoice && classChoice >= 1 && classChoice <= classCount) {
            break;
        }

        std::cout << "Please choose a valid class.\n";
        clearInput();
    }

    Equipment starterEquipment;
    starterEquipment.chestplate = "Starter Cloth Chestplate";
    starterEquipment.leggings = "Starter Cloth Pants";
    starterEquipment.boots = "Starter Cloth Boots";
    const std::string className = classes[classChoice - 1];
    character = {name, className, 0, 1, {}, starterEquipment,
                 generateAttributes(className, true), 0, 0};
    character.health = getCharacterMaxHealth(character);

    if (saveCharacter(character)) {
        std::cout << "Character saved.\n";
    } else {
        std::cout << "Warning: the character could not be saved.\n";
    }

    return true;
}

void showCharacterInfo(const Character& character) {
    const Attributes attributes = getCharacterAttributes(character);
    std::cout << "\n--- Character Stats ---\n";
    std::cout << "Name: " << character.name << '\n';
    std::cout << "Health: " << character.health << "/" << getCharacterMaxHealth(character) << '\n';
    std::cout << "Class: " << character.className << '\n';
    std::cout << "Level: " << character.level << '\n';
    std::cout << "Experience: " << character.experience << "/"
              << getExperienceToNextLevel(character.level) << '\n';
    std::cout << "Free Stat Points: " << character.freeStatPoints << '\n';
    std::cout << "Strength: " << attributes.strength << '\n';
    std::cout << "Agility: " << attributes.agility << '\n';
    std::cout << "Constitution: " << attributes.constitution << '\n';
    std::cout << "Spirit: " << attributes.spirit << '\n';
    std::cout << "Armor: " << getEquipmentArmor(character.equipment) << '\n';
    showEquipped(character.equipment);
}

void showEquipped(const Equipment& equipment) {
    auto gearRating = [](const std::string& item) {
        if (item.find("Starter Cloth") != std::string::npos) return 2;
        if (item.find("Basic ") != std::string::npos) return 3;
        return 0;
    };
    auto showSlot = [&gearRating](const std::string& slot, const std::string& item) {
        std::cout << slot << ": " << (item.empty() ? "Empty" : item);
        if (!item.empty()) {
            const int rating = gearRating(item);
            if (rating > 0) {
                std::cout << " (Gear rating: " << rating << ')';
            }
        }
        std::cout << '\n';
    };

    std::cout << "\n--- Equipped Items ---\n";
    showSlot("Helmet", equipment.helmet);
    showSlot("Chestplate", equipment.chestplate);
    showSlot("Bracers", equipment.bracers);
    showSlot("Leggings", equipment.leggings);
    showSlot("Boots", equipment.boots);
    showSlot("Necklace", equipment.necklace);
    showSlot("Bracelet 1", equipment.bracelets[0]);
    showSlot("Bracelet 2", equipment.bracelets[1]);
    for (std::size_t i = 0; i < equipment.rings.size(); ++i) {
        showSlot("Ring " + std::to_string(i + 1), equipment.rings[i]);
    }
}

enum class EquipmentType {
    None,
    Helmet,
    Chestplate,
    Bracers,
    Leggings,
    Boots,
    Necklace,
    Bracelet,
    Ring
};

EquipmentType getEquipmentType(const std::string& item) {
    if (item.find("Helmet") != std::string::npos) return EquipmentType::Helmet;
    if (item.find("Chestplate") != std::string::npos) return EquipmentType::Chestplate;
    if (item.find("Bracers") != std::string::npos) return EquipmentType::Bracers;
    if (item.find("Leggings") != std::string::npos ||
        item.find("Pants") != std::string::npos) return EquipmentType::Leggings;
    if (item.find("Boots") != std::string::npos) return EquipmentType::Boots;
    if (item.find("Necklace") != std::string::npos) return EquipmentType::Necklace;
    if (item.find("Bracelet") != std::string::npos) return EquipmentType::Bracelet;
    if (item.find("Ring") != std::string::npos) return EquipmentType::Ring;
    return EquipmentType::None;
}

int getGearRating(const std::string& item) {
    if (item.find("Starter Cloth") != std::string::npos) return 2;
    if (item.find("Basic ") != std::string::npos) return 3;
    return 0;
}

int getEquipmentArmor(const Equipment& equipment) {
    int armor = 0;
    const std::string equipmentItems[] = {
        equipment.helmet,
        equipment.chestplate,
        equipment.bracers,
        equipment.leggings,
        equipment.boots,
        equipment.necklace,
        equipment.bracelets[0],
        equipment.bracelets[1],
        equipment.rings[0],
        equipment.rings[1],
        equipment.rings[2],
        equipment.rings[3]
    };

    for (const std::string& item : equipmentItems) {
        armor += getGearRating(item);
    }

    return armor;
}

std::string getItemDescription(const std::string& item) {
    if (item == "Health Potion") {
        return "A small potion that restores health when used.";
    }
    if (item == "Iron Sword") {
        return "A sturdy sword suitable for a new adventurer.";
    }
    if (item == "Leather Armor") {
        return "Light armor that offers some protection.";
    }
    if (item == "Forest Map") {
        return "A hand-drawn map showing paths through the Whispering Woods.";
    }
    if (item == "Forest Crystal") {
        return "A glowing crystal found at the heart of the forest.";
    }

    const int rating = getGearRating(item);
    if (rating > 0) {
        return item.find("Starter Cloth") != std::string::npos
            ? "Simple cloth gear with a gear rating of 2."
            : "Basic adventuring gear with a gear rating of 3.";
    }

    if (getEquipmentType(item) != EquipmentType::None) {
        return "Wearable equipment with no recorded gear rating.";
    }

    return "A useful item collected during your adventure.";
}

void showItemDetails(const std::string& item) {
    std::cout << "\n--- Item Details ---\n";
    std::cout << "Name: " << item << '\n';
    std::cout << "Description: " << getItemDescription(item) << '\n';

    const int rating = getGearRating(item);
    if (rating > 0) {
        std::cout << "Gear rating: " << rating << '\n';
    }
}

bool chooseEquipmentSlot(const std::string& item, Equipment& equipment, std::string& replacedItem) {
    const EquipmentType type = getEquipmentType(item);
    replacedItem.clear();

    auto replaceSlot = [&replacedItem](std::string& slot, const std::string& itemToEquip) {
        replacedItem = slot;
        slot = itemToEquip;
    };

    switch (type) {
    case EquipmentType::Helmet:
        replaceSlot(equipment.helmet, item);
        return true;
    case EquipmentType::Chestplate:
        replaceSlot(equipment.chestplate, item);
        return true;
    case EquipmentType::Bracers:
        replaceSlot(equipment.bracers, item);
        return true;
    case EquipmentType::Leggings:
        replaceSlot(equipment.leggings, item);
        return true;
    case EquipmentType::Boots:
        replaceSlot(equipment.boots, item);
        return true;
    case EquipmentType::Necklace:
        replaceSlot(equipment.necklace, item);
        return true;
    case EquipmentType::Bracelet:
        for (std::string& bracelet : equipment.bracelets) {
            if (bracelet.empty()) {
                bracelet = item;
                return true;
            }
        }
        while (true) {
            std::cout << "Both bracelet slots are full. Replace slot 1, 2, or 0 to cancel: ";
            int slot;
            if (std::cin >> slot && slot >= 0 && slot <= 2) {
                if (slot == 0) return false;
                replaceSlot(equipment.bracelets[slot - 1], item);
                return true;
            }
            std::cout << "Please choose 0, 1, or 2.\n";
            clearInput();
        }
        break;
    case EquipmentType::Ring:
        for (std::string& ring : equipment.rings) {
            if (ring.empty()) {
                ring = item;
                return true;
            }
        }
        while (true) {
            std::cout << "All ring slots are full. Replace slot 1-4, or 0 to cancel: ";
            int slot;
            if (std::cin >> slot && slot >= 0 && slot <= 4) {
                if (slot == 0) return false;
                replaceSlot(equipment.rings[slot - 1], item);
                return true;
            }
            std::cout << "Please choose a number from 0 to 4.\n";
            clearInput();
        }
        break;
    case EquipmentType::None:
        return false;
    }

    return false;
}

void unequipMenu(Character& character) {
    while (true) {
        std::cout << "\nChoose an equipment slot to unequip:\n";
        std::cout << "1. Helmet: " << (character.equipment.helmet.empty() ? "Empty" : character.equipment.helmet) << '\n';
        std::cout << "2. Chestplate: " << (character.equipment.chestplate.empty() ? "Empty" : character.equipment.chestplate) << '\n';
        std::cout << "3. Bracers: " << (character.equipment.bracers.empty() ? "Empty" : character.equipment.bracers) << '\n';
        std::cout << "4. Leggings: " << (character.equipment.leggings.empty() ? "Empty" : character.equipment.leggings) << '\n';
        std::cout << "5. Boots: " << (character.equipment.boots.empty() ? "Empty" : character.equipment.boots) << '\n';
        std::cout << "6. Necklace: " << (character.equipment.necklace.empty() ? "Empty" : character.equipment.necklace) << '\n';
        std::cout << "7. Bracelet 1: " << (character.equipment.bracelets[0].empty() ? "Empty" : character.equipment.bracelets[0]) << '\n';
        std::cout << "8. Bracelet 2: " << (character.equipment.bracelets[1].empty() ? "Empty" : character.equipment.bracelets[1]) << '\n';
        std::cout << "9. Ring 1: " << (character.equipment.rings[0].empty() ? "Empty" : character.equipment.rings[0]) << '\n';
        std::cout << "10. Ring 2: " << (character.equipment.rings[1].empty() ? "Empty" : character.equipment.rings[1]) << '\n';
        std::cout << "11. Ring 3: " << (character.equipment.rings[2].empty() ? "Empty" : character.equipment.rings[2]) << '\n';
        std::cout << "12. Ring 4: " << (character.equipment.rings[3].empty() ? "Empty" : character.equipment.rings[3]) << '\n';
        std::cout << "0. Back to inventory\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return;
        }

        std::string* selectedItem = nullptr;
        switch (choice) {
        case 1: selectedItem = &character.equipment.helmet; break;
        case 2: selectedItem = &character.equipment.chestplate; break;
        case 3: selectedItem = &character.equipment.bracers; break;
        case 4: selectedItem = &character.equipment.leggings; break;
        case 5: selectedItem = &character.equipment.boots; break;
        case 6: selectedItem = &character.equipment.necklace; break;
        case 7: selectedItem = &character.equipment.bracelets[0]; break;
        case 8: selectedItem = &character.equipment.bracelets[1]; break;
        case 9: selectedItem = &character.equipment.rings[0]; break;
        case 10: selectedItem = &character.equipment.rings[1]; break;
        case 11: selectedItem = &character.equipment.rings[2]; break;
        case 12: selectedItem = &character.equipment.rings[3]; break;
        default:
            std::cout << "That is not a valid equipment slot.\n";
            continue;
        }

        if (selectedItem->empty()) {
            std::cout << "That equipment slot is empty.\n";
            continue;
        }

        const std::string removedItem = *selectedItem;
        character.inventory.push_back(removedItem);
        selectedItem->clear();
        saveCharacter(character);
        std::cout << removedItem << " was moved to your inventory.\n";
        return;
    }
}

void inventoryMenu(Character& character) {
    while (true) {
        showEquipped(character.equipment);
        std::cout << "\n--- Inventory ---\n";

        if (character.inventory.empty()) {
            std::cout << "Your inventory is empty.\n";
        } else {
            for (std::size_t i = 0; i < character.inventory.size(); ++i) {
                std::cout << i + 1 << ". " << character.inventory[i] << '\n';
            }
        }

        std::cout << "-1. Unequip an item\n";
        std::cout << "0. Back to character info\n";
        std::cout << "Choose an item to inspect: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return;
        }

        if (choice == -1) {
            unequipMenu(character);
            continue;
        }

        if (choice < 1 || choice > static_cast<int>(character.inventory.size())) {
            std::cout << "That is not a valid item. Try again.\n";
            continue;
        }

        const std::size_t itemIndex = static_cast<std::size_t>(choice - 1);
        const std::string item = character.inventory[itemIndex];
        showItemDetails(item);
        const EquipmentType type = getEquipmentType(item);

        if (type == EquipmentType::None) {
            continue;
        }

        std::cout << "1. Equip item\n";
        std::cout << "0. Back to inventory\n";
        std::cout << "Enter your choice: ";

        int itemChoice;
        if (!(std::cin >> itemChoice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (itemChoice == 0) {
            continue;
        }

        if (itemChoice != 1) {
            std::cout << "That is not a valid option.\n";
            continue;
        }

        std::string replacedItem;
        if (!chooseEquipmentSlot(item, character.equipment, replacedItem)) {
            std::cout << "All matching equipment slots are full. Remove or replace an item first.\n";
            continue;
        }

        character.inventory.erase(character.inventory.begin() + itemIndex);
        if (!replacedItem.empty()) {
            character.inventory.push_back(replacedItem);
        }
        saveCharacter(character);
        std::cout << item << " equipped.\n";
        if (!replacedItem.empty()) {
            std::cout << replacedItem << " was returned to your inventory.\n";
        }
    }
}

Attributes getCharacterAttributes(const Character& character) {
    return character.attributes;
}

int getCharacterMaxHealth(const Character& character) {
    return getCharacterAttributes(character).constitution * 10;
}

int getExperienceToNextLevel(int level) {
    int experienceNeeded = 10;
    for (int currentLevel = 1; currentLevel < level; ++currentLevel) {
        experienceNeeded *= 2;
    }
    return experienceNeeded;
}

int getExperienceReward(int playerLevel, int enemyLevel) {
    int reward = enemyLevel * 5;
    const int levelDifference = enemyLevel - playerLevel;

    if (levelDifference > 0) {
        reward += levelDifference * 5;
    } else if (levelDifference < 0) {
        reward = std::max(1, reward + levelDifference * 2);
    }

    return reward;
}

void awardExperience(Character& character, int enemyLevel) {
    const int reward = getExperienceReward(character.level, enemyLevel);
    character.experience += reward;
    std::cout << "You gained " << reward << " experience.\n";

    while (character.experience >= getExperienceToNextLevel(character.level)) {
        character.experience -= getExperienceToNextLevel(character.level);
        ++character.level;
        if (character.className == "Knight") {
            character.attributes.constitution += 2;
        } else if (character.className == "Hunter") {
            character.attributes.agility += 2;
        } else if (character.className == "Mage" || character.className == "Preist") {
            character.attributes.spirit += 2;
        } else if (character.className == "Swordsman") {
            character.attributes.strength += 2;
        }
        character.freeStatPoints += 2;
        std::cout << "Level up! You are now level " << character.level << ".\n";
        std::cout << "You gained 2 class attribute points and 2 free stat points.\n";
    }
}

int getAttackAttribute(const Character& character) {
    if (character.className == "Mage" || character.className == "Preist") {
        return getCharacterAttributes(character).spirit;
    }

    return getCharacterAttributes(character).strength;
}

Enemy generateEnemy(const std::string& areaName, int depth) {
    static std::mt19937 generator(std::random_device{}());
    std::uniform_int_distribution<int> levelVariation(0, 1);
    const int enemyLevel = std::max(1, depth + levelVariation(generator));

    std::string enemyName;
    if (depth <= 1) {
        enemyName = areaName == "The Village" ? "Wild Rat" : "Forest Wolf";
    } else if (depth == 2) {
        enemyName = "Cave Bear";
    } else if (depth == 3) {
        enemyName = "Ancient Treant";
    } else {
        enemyName = "Forest Guardian";
    }

    const int maxHealth = 10 + enemyLevel * 10;
    return {
        enemyName,
        enemyLevel,
        maxHealth,
        maxHealth,
        std::max(0, enemyLevel - 1),
        {enemyLevel, enemyLevel, enemyLevel, enemyLevel}
    };
}

bool combat(Character& character, Enemy enemy) {
    static std::mt19937 generator(std::random_device{}());
    std::uniform_int_distribution<int> damageVariation(0, 2);
    std::uniform_int_distribution<int> dodgeRoll(1, 100);
    bool defending = false;

    character.health = std::min(character.health, getCharacterMaxHealth(character));
    std::cout << "\nA level " << enemy.level << " " << enemy.name << " appears!\n";
    std::cout << "Enemy armor: " << enemy.armor << '\n';

    while (character.health > 0 && enemy.health > 0) {
        const Attributes playerAttributes = getCharacterAttributes(character);
        std::cout << "\nYour health: " << character.health << "/" << getCharacterMaxHealth(character)
                  << " | " << enemy.name << " health: " << enemy.health << "/" << enemy.maxHealth << '\n';
        std::cout << "1. Attack\n";
        std::cout << "2. Defend\n";
        std::cout << "3. Run\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 3) {
            character.health = getCharacterMaxHealth(character);
            saveCharacter(character);
            std::cout << "You escaped and returned to the main Explore menu.\n";
            return true;
        }

        if (choice == 1) {
            const int attackStat = getAttackAttribute(character);
            const int damage = std::max(
                1,
                attackStat * 3 / 2
                    - enemy.attributes.constitution
                    - enemy.armor
                    + damageVariation(generator)
            );
            enemy.health -= damage;
            defending = false;
            std::cout << "You dealt " << damage << " damage.\n";
        } else if (choice == 2) {
            defending = true;
            std::cout << "You brace yourself for the enemy's attack.\n";
        } else {
            std::cout << "That is not a valid combat option.\n";
            continue;
        }

        if (enemy.health <= 0) {
            std::cout << "You defeated the " << enemy.name << "!\n";
            break;
        }

        const int playerArmor = getEquipmentArmor(character.equipment);
        int enemyDamage = std::max(
            0,
            enemy.attributes.strength * 3 / 2
                - playerAttributes.constitution
                - playerArmor
                + damageVariation(generator)
        );
        if (defending) {
            enemyDamage = std::max(0, enemyDamage / 2);
        }

        if (enemyDamage > 0 &&
            dodgeRoll(generator) <= std::min(50, playerAttributes.agility * 3)) {
            std::cout << "You dodged the attack!\n";
            continue;
        }

        character.health -= enemyDamage;
        std::cout << "The " << enemy.name << " dealt " << enemyDamage << " damage.\n";
    }

    if (enemy.health <= 0) {
        awardExperience(character, enemy.level);
    } else if (character.health <= 0) {
        std::cout << "You were defeated, but the adventure continues.\n";
    }

    character.health = getCharacterMaxHealth(character);
    saveCharacter(character);
    std::cout << "Your health has been restored to full.\n";
    return false;
}

bool findCreature(Character& character, const std::string& areaName, int depth) {
    std::cout << "\nYou search " << areaName << " for a creature.\n";
    return combat(character, generateEnemy(areaName, depth));
}

void distributeFreeStatPoints(Character& character) {
    while (character.freeStatPoints > 0) {
        const Attributes attributes = getCharacterAttributes(character);
        std::cout << "\nFree stat points: " << character.freeStatPoints << '\n';
        std::cout << "1. Strength (" << attributes.strength << ")\n";
        std::cout << "2. Agility (" << attributes.agility << ")\n";
        std::cout << "3. Constitution (" << attributes.constitution << ")\n";
        std::cout << "4. Spirit (" << attributes.spirit << ")\n";
        std::cout << "0. Back\n";
        std::cout << "Choose a stat to increase: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return;
        }

        switch (choice) {
        case 1:
            ++character.attributes.strength;
            break;
        case 2:
            ++character.attributes.agility;
            break;
        case 3:
            ++character.attributes.constitution;
            break;
        case 4:
            ++character.attributes.spirit;
            break;
        default:
            std::cout << "That is not a valid stat.\n";
            continue;
        }

        --character.freeStatPoints;
        saveCharacter(character);
    }

    std::cout << "All free stat points have been distributed.\n";
}

void characterInfoMenu(Character& character) {
    while (true) {
        std::cout << "\n--- Character Info ---\n";
        std::cout << "1. View stats\n";
        std::cout << "2. View inventory\n";
        if (character.freeStatPoints > 0) {
            std::cout << "3. Distribute stat points\n";
        }
        std::cout << "0. Back to main menu\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return;
        }

        if (choice == 1) {
            showCharacterInfo(character);
        } else if (choice == 2) {
            inventoryMenu(character);
        } else if (choice == 3 && character.freeStatPoints > 0) {
            distributeFreeStatPoints(character);
        } else {
            std::cout << "That is not a valid option. Try again.\n";
        }
    }
}

void showMerchant(Character& character) {
    const std::string items[] = {
        "Health Potion",
        "Iron Sword",
        "Leather Armor",
        "Basic Helmet",
        "Basic Chestplate",
        "Basic Bracers",
        "Basic Leggings",
        "Basic Boots",
        "Basic Necklace",
        "Basic Bracelet",
        "Basic Ring"
    };
    constexpr int itemCount = sizeof(items) / sizeof(items[0]);

    while (true) {
        std::cout << "\n--- Village Merchant ---\n";
        std::cout << "Choose an item to add to your inventory:\n";
        for (int i = 0; i < itemCount; ++i) {
            std::cout << i + 1 << ". " << items[i] << '\n';
        }
        std::cout << "0. Back to village\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return;
        }

        if (choice < 1 || choice > itemCount) {
            std::cout << "That is not a valid item. Try again.\n";
            continue;
        }

        character.inventory.push_back(items[choice - 1]);
        saveCharacter(character);
        std::cout << items[choice - 1] << " was added to your inventory.\n";
    }
}

bool exploreVillage(Character& character) {
    while (true) {
        std::cout << "\n--- The Village ---\n";
        std::cout << "1. Merchant\n";
        std::cout << "2. Village Square\n";
        std::cout << "3. Find a creature\n";
        std::cout << "0. Back to main menu\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return false;
        }

        if (choice == 1) {
            showMerchant(character);
        } else if (choice == 2) {
            std::cout << "The village square is busy with travelers and traders.\n";
        } else if (choice == 3) {
            if (findCreature(character, "The Village", 1)) {
                return true;
            }
        } else {
            std::cout << "That is not a valid location. Try again.\n";
        }
    }
}

bool exploreDeepForest(Character& character, int depth) {
    const std::string areaNames[] = {
        "The Deep Forest",
        "The Ancient Grove",
        "The Forest Heart"
    };

    std::cout << "\nYou travel deeper into " << areaNames[depth] << ".\n";
    if (depth == 2 && std::find(character.inventory.begin(), character.inventory.end(), "Forest Crystal") == character.inventory.end()) {
        std::cout << "A strange crystal rests at the center of the forest.\n";
        character.inventory.push_back("Forest Crystal");
        saveCharacter(character);
        std::cout << "You picked up a Forest Crystal.\n";
    }

    while (true) {
        std::cout << "\n--- " << areaNames[depth] << " ---\n";
        if (depth < 2) {
            std::cout << "1. Go deeper\n";
            std::cout << "2. Find a creature\n";
            std::cout << "3. Look around\n";
        } else {
            std::cout << "1. Find a creature\n";
            std::cout << "2. Look around\n";
        }
        std::cout << "0. Back to main menu\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return false;
        }

        if (depth < 2 && choice == 1) {
            if (exploreDeepForest(character, depth + 1)) {
                return true;
            }
        } else if ((depth < 2 && choice == 2) || (depth == 2 && choice == 1)) {
            if (findCreature(character, areaNames[depth], depth + 2)) {
                return true;
            }
        } else if ((depth < 2 && choice == 3) || (depth == 2 && choice == 2)) {
            std::cout << "The trees grow thicker, and the path ahead becomes harder to see.\n";
        } else {
            std::cout << "That is not a valid option. Try again.\n";
        }
    }
}

bool exploreForest(Character& character) {
    while (true) {
        std::cout << "\n--- The Whispering Woods ---\n";
        std::cout << "1. Enter the Deep Forest\n";
        std::cout << "2. Search the forest edge\n";
        std::cout << "3. Find a creature\n";
        std::cout << "0. Back to main menu\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return false;
        }

        if (choice == 1) {
            if (exploreDeepForest(character, 0)) {
                return true;
            }
        } else if (choice == 2) {
            std::cout << "You find a quiet trail leading farther into the woods.\n";
            if (std::find(character.inventory.begin(), character.inventory.end(), "Forest Map") == character.inventory.end()) {
                character.inventory.push_back("Forest Map");
                saveCharacter(character);
                std::cout << "You picked up a Forest Map.\n";
            }
        } else if (choice == 3) {
            if (findCreature(character, "the forest edge", 1)) {
                return true;
            }
        } else {
            std::cout << "That is not a valid location. Try again.\n";
        }
    }
}

bool exploreStandardLocation(Character& character, const Location& location, int depth) {
    while (true) {
        std::cout << "\n--- " << location.name << " ---\n";
        std::cout << "1. Find a creature\n";
        std::cout << "2. Look around\n";
        std::cout << "0. Back to main menu\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return false;
        }

        if (choice == 1) {
            if (findCreature(character, location.name, depth)) {
                return true;
            }
        } else if (choice == 2) {
            std::cout << location.description << '\n';
        } else {
            std::cout << "That is not a valid option. Try again.\n";
        }
    }
}

void explore(Character& character, const Location locations[], int locationCount) {
    while (true) {
        std::cout << "\nWhere would you like to go?\n";

        for (int i = 0; i < locationCount; ++i) {
            std::cout << i + 1 << ". " << locations[i].name << '\n';
        }

        std::cout << "0. Back to main menu\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            return;
        }

        if (choice < 1 || choice > locationCount) {
            std::cout << "That is not a valid location. Try again.\n";
            continue;
        }

        const Location& destination = locations[choice - 1];
        if (choice == 1) {
            exploreVillage(character);
        } else if (choice == 2) {
            exploreForest(character);
        } else {
            exploreStandardLocation(character, destination, 2);
        }
    }
}

void playCharacter(Character& character, const Location locations[], int locationCount) {
    while (true) {
        std::cout << "\nWhat would you like to do?\n";
        std::cout << "1. Character info\n";
        std::cout << "2. Explore\n";
        std::cout << "0. Back to main menu\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        switch (choice) {
        case 1:
            characterInfoMenu(character);
            break;
        case 2:
            explore(character, locations, locationCount);
            return;
        case 0:
            std::cout << "Returning to main menu.\n";
            return;
        default:
            std::cout << "That is not a valid option. Try again.\n";
        }
    }
}

int main() {
    const Location locations[] = {
        {"The Village", "A small village sits quietly beneath the hills."},
        {"The Whispering Woods", "The trees sway even though there is no wind."},
        {"The Old Ruins", "Broken stone walls hint at a forgotten kingdom."},
        {"The Moonlit Lake", "The lake reflects the sky like a silver mirror."}
    };
    constexpr int locationCount = sizeof(locations) / sizeof(locations[0]);

    std::cout << "=================================\n";
    std::cout << "       A Tiny Terminal RPG        \n";
    std::cout << "=================================\n";
    std::cout << "Welcome, adventurer!\n";

    Character character;
    while (true) {
        std::cout << "\n1. Load character\n";
        std::cout << "2. Make new character\n";
        std::cout << "3. Delete character\n";
        std::cout << "0. Quit game\n";
        std::cout << "Enter your choice: ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cout << "Please enter a number.\n";
            clearInput();
            continue;
        }

        if (choice == 0) {
            std::cout << "Thanks for playing!\n";
            return 0;
        }

        if (choice == 1) {
            if (loadCharacter(character)) {
                std::cout << "Character loaded.\n";
                playCharacter(character, locations, locationCount);
            } else {
                std::cout << "No valid character save was found.\n";
            }
        } else if (choice == 2) {
            if (createCharacter(character)) {
                playCharacter(character, locations, locationCount);
            }
        } else if (choice == 3) {
            deleteCharacter();
        } else {
            std::cout << "That is not a valid option. Try again.\n";
        }
    }

    return 0;
}
