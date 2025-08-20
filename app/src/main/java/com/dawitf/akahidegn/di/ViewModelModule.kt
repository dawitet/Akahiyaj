package com.dawitf.akahidegn.di

import androidx.lifecycle.ViewModel
import com.dawitf.akahidegn.ui.profile.ProfileFeatureViewModel
import com.dawitf.akahidegn.ui.social.RideBuddyViewModel
import com.dawitf.akahidegn.ui.social.SocialViewModel
import com.dawitf.akahidegn.viewmodel.MainViewModel
import com.dawitf.akahidegn.viewmodel.SettingsViewModel

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileFeatureViewModel::class)
    abstract fun bindProfileFeatureViewModel(viewModel: ProfileFeatureViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RideBuddyViewModel::class)
    abstract fun bindRideBuddyViewModel(viewModel: RideBuddyViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SocialViewModel::class)
    abstract fun bindSocialViewModel(viewModel: SocialViewModel): ViewModel
}