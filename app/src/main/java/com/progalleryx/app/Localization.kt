package com.progalleryx.app

object Localization {

    fun appTitle(isAr: Boolean) = if (isAr) "Pro Gallery X" else "Pro Gallery X"
    fun appSubtitle(isAr: Boolean) = if (isAr) "ذكرياتك، على هاتفك" else "Your memories, safely on device"

    fun tabPictures(isAr: Boolean) = if (isAr) "الصور" else "Pictures"
    fun tabAlbums(isAr: Boolean) = if (isAr) "الألبومات" else "Albums"
    fun tabStudio(isAr: Boolean) = if (isAr) "الاستوديو" else "Studio"
    fun tabFavorites(isAr: Boolean) = if (isAr) "المفضلة" else "Favorites"

    fun filterAll(isAr: Boolean) = if (isAr) "الكل" else "All"
    fun filterPhotos(isAr: Boolean) = if (isAr) "الصور" else "Photos"
    fun filterVideos(isAr: Boolean) = if (isAr) "الفيديوهات" else "Videos"
    fun filterFavorites(isAr: Boolean) = if (isAr) "المفضلة" else "Favorites"

    fun sortNewest(isAr: Boolean) = if (isAr) "الأحدث" else "Newest"
    fun sortOldest(isAr: Boolean) = if (isAr) "الأقدم" else "Oldest"

    fun searchPlaceholder(isAr: Boolean) = if (isAr) "بحث باسم الصورة أو الفيديو..." else "Search photos & videos..."
    fun clearSearch(isAr: Boolean) = if (isAr) "مسح البحث" else "Clear search"

    fun permissionTitle(isAr: Boolean) = if (isAr) "صورك تبقى على هاتفك" else "Your photos stay on your device"
    fun permissionBody(isAr: Boolean) = if (isAr) "امنح إذن الوصول للوسائط لعرض الصور والفيديوهات محليًا وبسرعة فائقة." else "Grant media access to view your photos and videos locally with high speed."
    fun permissionButton(isAr: Boolean) = if (isAr) "السماح بالوصول" else "Allow Access"
    fun limitedPermission(isAr: Boolean) = if (isAr) "الوصول محدود للصور المختارة فقط. يمكنك توسيع الإذن من الإعدادات." else "Limited access to selected photos only. Expand in device settings."
    fun updatePermission(isAr: Boolean) = if (isAr) "تحديث الإذن" else "Update Access"

    fun selectMode(isAr: Boolean) = if (isAr) "تحديد" else "Select"
    fun selectedCount(count: Int, isAr: Boolean) = if (isAr) "تم تحديد $count" else "$count selected"
    fun selectAll(isAr: Boolean) = if (isAr) "تحديد الكل" else "Select All"
    fun deselectAll(isAr: Boolean) = if (isAr) "إلغاء التحديد" else "Deselect"

    fun share(isAr: Boolean) = if (isAr) "مشاركة" else "Share"
    fun delete(isAr: Boolean) = if (isAr) "إلى السلة" else "Trash"
    fun confirmTrashTitle(isAr: Boolean) = if (isAr) "نقل إلى سلة المحذوفات؟" else "Move to Trash?"
    fun confirmTrashBody(isAr: Boolean) = if (isAr) "سيطلب أندرويد تأكيدك قبل نقل الملفات إلى السلة. لن تُحذف نهائيًا هنا." else "Android will ask for confirmation before trashing. Files will not be deleted permanently."
    fun continueAction(isAr: Boolean) = if (isAr) "متابعة" else "Continue"
    fun cancel(isAr: Boolean) = if (isAr) "إلغاء" else "Cancel"

    fun edit(isAr: Boolean) = if (isAr) "تعديل" else "Edit"
    fun details(isAr: Boolean) = if (isAr) "التفاصيل" else "Details"
    fun favorite(isAr: Boolean) = if (isAr) "مفضلة" else "Favorite"
    fun close(isAr: Boolean) = if (isAr) "إغلاق" else "Close"
    fun back(isAr: Boolean) = if (isAr) "رجوع" else "Back"
    fun refresh(isAr: Boolean) = if (isAr) "تحديث المعرض" else "Refresh"

    fun emptyGallery(isAr: Boolean) = if (isAr) "لا توجد صور أو فيديوهات" else "No photos or videos"
    fun emptySearch(isAr: Boolean) = if (isAr) "لا توجد نتائج مطابقة للبحث" else "No matching results"
    fun emptyFavorites(isAr: Boolean) = if (isAr) "لم تضف عناصر إلى المفضلة بعد" else "No favorites added yet"
    fun emptyAlbum(isAr: Boolean) = if (isAr) "هذا الألبوم فارغ" else "This album is empty"

    fun allAlbums(isAr: Boolean) = if (isAr) "جميع الألبومات" else "All Albums"
    fun albumItemsCount(count: Int, isAr: Boolean) = if (isAr) "$count عنصر" else "$count items"

    fun theme(isAr: Boolean) = if (isAr) "المظهر" else "Theme"
    fun themeSystem(isAr: Boolean) = if (isAr) "حسب النظام" else "System"
    fun themeLight(isAr: Boolean) = if (isAr) "فاتح" else "Light"
    fun themeDark(isAr: Boolean) = if (isAr) "داكن" else "Dark"

    fun language(isAr: Boolean) = if (isAr) "اللغة" else "Language"
    fun languageSystem(isAr: Boolean) = if (isAr) "حسب الجهاز" else "System"
    fun languageArabic(isAr: Boolean) = "العربية"
    fun languageEnglish(isAr: Boolean) = "English"

    fun detailsType(isAr: Boolean) = if (isAr) "النوع" else "Type"
    fun detailsSize(isAr: Boolean) = if (isAr) "الحجم" else "Size"
    fun detailsDate(isAr: Boolean) = if (isAr) "التاريخ" else "Date"
    fun detailsPath(isAr: Boolean) = if (isAr) "المسار" else "Location"
    fun detailsResolution(isAr: Boolean) = if (isAr) "الأبعاد" else "Resolution"
    fun detailsDuration(isAr: Boolean) = if (isAr) "المدة" else "Duration"

    fun typePhoto(isAr: Boolean) = if (isAr) "صورة" else "Photo"
    fun typeVideo(isAr: Boolean) = if (isAr) "فيديو" else "Video"

    fun studioHubTitle(isAr: Boolean) = if (isAr) "استوديو الإبداع" else "Creative Studio"
    fun photoStudioTitle(isAr: Boolean) = if (isAr) "استوديو الصور" else "Photo Studio"
    fun videoStudioTitle(isAr: Boolean) = if (isAr) "استوديو الفيديو" else "Video Studio"
    fun pdfSuiteTitle(isAr: Boolean) = if (isAr) "أدوات المستندات وPDF" else "PDF Document Suite"
    fun storageCleanerTitle(isAr: Boolean) = if (isAr) "منظف ومحرر التخزين" else "Storage & Duplicate Cleaner"

    fun compareHold(isAr: Boolean) = if (isAr) "قبل / بعد" else "Before / After"
    fun saveToGallery(isAr: Boolean) = if (isAr) "حفظ في المعرض" else "Save to Gallery"

    fun errorUnsupported(isAr: Boolean) = if (isAr) "تعذر تشغيل هذا الملف على الجهاز" else "Unable to play this file on device"
}
