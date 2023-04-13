package org.aiotrade.lib.securities.data.git

import org.aiotrade.lib.util.nls.NLS
import org.aiotrade.lib.util.nls.TranslationBundle

class CLIText extends TranslationBundle {
  var IPZillaPasswordPrompt: String = _
  var authorInfo: String = _
  var averageMSPerRead: String = _
  var branchAlreadyExists: String = _
  var branchCreatedFrom: String = _
  var branchDetachedHEAD: String = _
  var branchIsNotAnAncestorOfYourCurrentHEAD: String = _
  var branchNotFound: String = _
  var cacheTreePathInfo: String = _
  var configFileNotFound: String = _
  var cannotBeRenamed: String = _
  var cannotChekoutNoHeadsAdvertisedByRemote: String = _
  var cannotCreateCommand: String = _
  var cannotCreateOutputStream: String = _
  var cannotDeatchHEAD: String = _
  var cannotDeleteTheBranchWhichYouAreCurrentlyOn: String = _
  var cannotGuessLocalNameFrom: String = _
  var cannotLock: String = _
  var cannotReadBecause: String = _
  var cannotReadPackageInformation: String = _
  var cannotRenameDetachedHEAD: String = _
  var cannotResolve: String = _
  var cannotSetupConsole: String = _
  var cannotUseObjectsWithGlog: String = _
  var cannotWrite: String = _
  var cantFindGitDirectory: String = _
  var cantWrite: String = _
  var commitLabel: String = _
  var conflictingUsageOf_git_dir_andArguments: String = _
  var couldNotCreateBranch: String = _
  var dateInfo: String = _
  var deletedBranch: String = _
  var deletedRemoteBranch: String = _
  var doesNotExist: String = _
  var everythingUpToDate: String = _
  var expectedNumberOfbytes: String = _
  var exporting: String = _
  var failedToCommitIndex: String = _
  var failedToLockIndex: String = _
  var failedToLockTag: String = _
  var fatalError: String = _
  var fatalErrorTagExists: String = _
  var fatalThisProgramWillDestroyTheRepository: String = _
  var forcedUpdate: String = _
  var fromURI: String = _
  var initializedEmptyGitRepositoryIn: String = _
  var invalidHttpProxyOnlyHttpSupported: String = _
  var jgitVersion: String = _
  var listeningOn: String = _
  var metaVar_command: String = _
  var metaVar_commitish: String = _
  var metaVar_object: String = _
  var metaVar_paths: String = _
  var metaVar_refspec: String = _
  var metaVar_treeish: String = _
  var mostCommonlyUsedCommandsAre: String = _
  var needApprovalToDestroyCurrentRepository: String = _
  var noGitRepositoryConfigured: String = _
  var noSuchFile: String = _
  var noTREESectionInIndex: String = _
  var nonFastForward: String = _
  var notABranch: String = _
  var notACommit: String = _
  var notAGitRepository: String = _
  var notAJgitCommand: String = _
  var notARevision: String = _
  var notATagVersionIsRequired: String = _
  var notATree: String = _
  var notAValidRefName: String = _
  var notAnIndexFile: String = _
  var notAnObject: String = _
  var notFound: String = _
  var noteObjectTooLargeToPrint: String = _
  var onlyOneMetaVarExpectedIn: String = _
  var pushTo: String = _
  var pathsRequired: String = _
  var remoteMessage: String = _
  var remoteRefObjectChangedIsNotExpectedOne: String = _
  var remoteSideDoesNotSupportDeletingRefs: String = _
  var repaint: String = _
  var serviceNotSupported: String = _
  var skippingObject: String = _
  var timeInMilliSeconds: String = _
  var tooManyRefsGiven: String = _
  var unsupportedOperation: String = _
  var warningNoCommitGivenOnCommandLine: String = _
}

object CLIText {
  def apply() = NLS.getBundleFor(classOf[CLIText])
}
