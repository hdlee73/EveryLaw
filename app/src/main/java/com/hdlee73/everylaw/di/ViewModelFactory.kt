package com.hdlee73.everylaw.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hdlee73.everylaw.ui.library.DocumentViewModel
import com.hdlee73.everylaw.ui.library.PrecedentViewModel
import com.hdlee73.everylaw.ui.search.SearchViewModel
import com.hdlee73.everylaw.ui.settings.SettingsViewModel

fun AppContainer.viewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer { SearchViewModel(getDelegationTree, searchKeyword, getLawVersions, buildPdfExtras, settings) }
    initializer { PrecedentViewModel(getPrecedent) }
    initializer { DocumentViewModel(getLawArticles, getAdminRule) }
    initializer { SettingsViewModel(settings, updateManager) }
}
